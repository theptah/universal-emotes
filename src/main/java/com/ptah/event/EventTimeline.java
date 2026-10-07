package com.ptah.event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EventTimeline {
    private final List<EmoteEvent> events;
    public EventTimeline(List<EmoteEvent> events) { this.events = events.stream().sorted(Comparator.comparingDouble(EmoteEvent::time)).toList(); }
    public List<EmoteEvent> events() { return events; }

    public List<EmoteEvent> crossed(double previous, double current, com.ptah.animation.AnimationClip clip) {
        float loopStart = clip.loopStart();
        if (!clip.loop() || loopStart <= 0f) return crossed(previous, current, clip.length(), clip.loop());
        if (current < previous || events.isEmpty()) return List.of();
        float length = clip.length();
        List<EmoteEvent> result = new ArrayList<>();

        double capped = Math.min(current, length);
        for (EmoteEvent event : events) if (event.time() > previous && event.time() <= capped) result.add(event);
        if (current < length) return result;

        double span = length - loopStart;
        long firstCycle = Math.max(0, (long) Math.floor((Math.max(previous, length) - length) / span));
        long lastCycle = (long) Math.floor((current - length) / span);
        if (lastCycle - firstCycle > 128) firstCycle = lastCycle - 128;
        for (long cycle = firstCycle; cycle <= lastCycle; cycle++) {
            double base = length + cycle * span - loopStart;
            for (EmoteEvent event : events) {
                if (event.time() < loopStart) continue;
                double occurrence = base + event.time();
                if (occurrence > previous && occurrence <= current && occurrence >= length) result.add(event);
            }
        }
        return result;
    }

    public List<EmoteEvent> crossed(double previous, double current, float length, boolean loop) {
        if (current < previous || events.isEmpty()) return List.of();
        List<EmoteEvent> result = new ArrayList<>();
        if (!loop) {
            double capped = Math.min(current, length);
            for (EmoteEvent event : events) if (event.time() > previous && event.time() <= capped) result.add(event);
            return result;
        }
        long firstCycle = Math.max(0, (long) Math.floor(Math.max(previous, 0) / length));
        long lastCycle = Math.max(0, (long) Math.floor(current / length));
        if (lastCycle - firstCycle > 128) firstCycle = lastCycle - 128;
        for (long cycle = firstCycle; cycle <= lastCycle; cycle++) {
            double base = cycle * (double) length;
            for (EmoteEvent event : events) {
                double occurrence = base + event.time();
                if (occurrence > previous && occurrence <= current) result.add(event);
            }
        }
        return result;
    }
}
