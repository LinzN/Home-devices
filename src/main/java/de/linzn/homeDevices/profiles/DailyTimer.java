package de.linzn.homeDevices.profiles;

import de.linzn.stem.api.scheduler.ScheduledTask;
import de.linzn.stem.modules.pluginModule.STEMPlugin;

import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * Runs an action at fixed times of the day with millisecond precision.
 *
 * <p>Replaces the former busy loops (one thread per device polling every 50 ms forever): only one
 * delayed task is pending at a time, it is owned by the plugin and cancelled on reload or disable.
 *
 * @param <V> value attached to each time, e.g. switch state or target temperature
 */
public final class DailyTimer<V> {

    private final STEMPlugin plugin;
    private final String name;
    private final BiConsumer<LocalTime, V> action;
    /* sorted by time, replaced as a whole on reload */
    private volatile List<Map.Entry<LocalTime, V>> entries = List.of();
    private ScheduledTask pendingTask;
    private boolean active;

    public DailyTimer(STEMPlugin plugin, String name, BiConsumer<LocalTime, V> action) {
        this.plugin = plugin;
        this.name = name;
        this.action = action;
    }

    /**
     * Replaces all timer entries. Does not schedule anything until {@link #start()} was called.
     */
    public synchronized void setEntries(List<Map.Entry<LocalTime, V>> newEntries) {
        List<Map.Entry<LocalTime, V>> sorted = new ArrayList<>(newEntries);
        sorted.sort(Comparator.comparing(Map.Entry::getKey));
        this.entries = List.copyOf(sorted);
        if (this.active) {
            this.scheduleNext(ZonedDateTime.now(this.zone()));
        }
    }

    public synchronized void start() {
        this.active = true;
        this.scheduleNext(ZonedDateTime.now(this.zone()));
    }

    public synchronized void stop() {
        this.active = false;
        if (this.pendingTask != null) {
            this.pendingTask.cancel();
            this.pendingTask = null;
        }
    }

    /**
     * @return the value of the last entry that is due today before now, or of the last entry of the day
     * if no entry is due yet today. Null if there are no entries.
     */
    public V getCurrentValue() {
        List<Map.Entry<LocalTime, V>> list = this.entries;
        if (list.isEmpty()) {
            return null;
        }
        LocalTime now = LocalTime.now(this.zone());
        V value = list.get(list.size() - 1).getValue();
        for (Map.Entry<LocalTime, V> entry : list) {
            if (entry.getKey().isAfter(now)) {
                break;
            }
            value = entry.getValue();
        }
        return value;
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /* same clock as the timer configuration was written for (formerly LocalTime.now()) */
    private ZoneId zone() {
        return ZoneId.systemDefault();
    }

    /* caller holds the lock */
    private void scheduleNext(ZonedDateTime notBefore) {
        if (this.pendingTask != null) {
            this.pendingTask.cancel();
            this.pendingTask = null;
        }
        List<Map.Entry<LocalTime, V>> list = this.entries;
        if (!this.active || list.isEmpty()) {
            return;
        }
        ZoneId zone = notBefore.getZone();
        ZonedDateTime fireAt = null;
        for (int day = 0; day <= 1 && fireAt == null; day++) {
            for (Map.Entry<LocalTime, V> entry : list) {
                ZonedDateTime candidate = notBefore.toLocalDate().plusDays(day).atTime(entry.getKey()).atZone(zone);
                if (candidate.isAfter(notBefore)) {
                    fireAt = candidate;
                    break;
                }
            }
        }
        if (fireAt == null) {
            return;
        }
        final ZonedDateTime scheduledFor = fireAt;
        long delay = Math.max(0, Duration.between(ZonedDateTime.now(zone), fireAt).toMillis());
        this.pendingTask = this.plugin.getStemApi().scheduler().runLater(() -> this.fire(scheduledFor), delay, TimeUnit.MILLISECONDS);
    }

    private void fire(ZonedDateTime scheduledFor) {
        LocalTime fireTime = scheduledFor.toLocalTime();
        try {
            for (Map.Entry<LocalTime, V> entry : this.entries) {
                if (entry.getKey().equals(fireTime)) {
                    this.action.accept(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            this.plugin.getPluginLogger().error("Timer " + this.name + " failed at " + fireTime, e);
        } finally {
            synchronized (this) {
                this.pendingTask = null;
                /* never schedule the same entry twice, even if the task woke up a few ms early */
                ZonedDateTime now = ZonedDateTime.now(scheduledFor.getZone());
                this.scheduleNext(now.isAfter(scheduledFor) ? now : scheduledFor);
            }
        }
    }
}
