package com.zachoverflow.gabeldorsche;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Vibe {
    public enum Location {
        FRONT_LEFT(0),
        FRONT_RIGHT(1),
        BACK_RIGHT(2),
        BACK_LEFT(3);

        private final int value;
        private Location(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    Sequence[] sequences = new Sequence[Location.values().length];

    public Vibe() {
        for (int i = 0; i < sequences.length; i++)
            sequences[i] = new Sequence();
    }

    public short getDurationMillis() {
        short longestDuration = 0;

        for (Sequence sequence : sequences) {
            short duration = sequence.getDurationMillis();
            if (duration > longestDuration)
                longestDuration = duration;
        }

        return longestDuration;
    }

    public int getSerializedLength() {
        int length = 0;
        for (Sequence sequence : sequences)
            length += sequence.getSerializedLength();

        return length;
    }

    public void serializeTo(ByteBuffer buffer) {
        for (Sequence sequence : sequences)
            sequence.serializeTo(buffer);
    }

    public Sequence at(Location location) {
        return sequences[location.getValue()];
    }

    public static class Sequence {
        ArrayList<Step> steps = new ArrayList<>();

        public Sequence add(float value, short durationMillis) {
            this.steps.add(new Step(value, durationMillis));
            return this;
        }

        public Sequence add(Step step) {
            return this.add(step.value, step.durationMillis);
        }

        public short getDurationMillis() {
            short duration = 0;

            for (Step step : steps)
                duration += step.durationMillis;

            return duration;
        }

        private int getSerializedLength() {
            return steps.size() * Step.SERIALIZED_LENGTH + 2;
        }

        private void serializeTo(ByteBuffer buffer) {
            buffer.putShort((short)this.steps.size());

            for (Step step : this.steps)
                step.serializeTo(buffer);
        }
    }

    public static class Step {
        private static final int SERIALIZED_LENGTH = 6;

        float value;
        short durationMillis;

        public Step(float value, short durationMillis) {
            this.value = value;
            this.durationMillis = durationMillis;
        }

        private void serializeTo(ByteBuffer buffer) {
            buffer.putFloat(this.value);
            buffer.putShort(this.durationMillis);
        }
    }

    public static Vibe concatenate(short delayMillis, Vibe... vibes) {
        Vibe finished = null;

        for (Vibe vibe : vibes) {
            if (vibe == null)
                continue;

            if (finished != null) {
                for (Sequence sequence : finished.sequences)
                    sequence.add(0.0f, delayMillis);
            } else {
                finished = new Vibe();
            }

            short vibeDurationMillis = vibe.getDurationMillis();
            for (int i = 0; i < finished.sequences.length; i++) {
                Sequence finishedSequence = finished.sequences[i];
                short sequenceDurationMillis = 0;

                for (Step step : vibe.sequences[i].steps) {
                    finishedSequence.add(step);
                    sequenceDurationMillis += step.durationMillis;
                }

                short millisLeft = (short)(vibeDurationMillis - sequenceDurationMillis);
                if (millisLeft > 0)
                    finishedSequence.add(0.0f, millisLeft);
            }
        }

        return finished;
    }
}
