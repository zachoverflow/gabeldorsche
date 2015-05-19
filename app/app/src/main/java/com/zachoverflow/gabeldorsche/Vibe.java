package com.zachoverflow.gabeldorsche;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class Vibe {
    public enum Location {
        FRONT_RIGHT(0),
        FRONT_LEFT(1),
        BACK_LEFT(2),
        BACK_RIGHT(3);

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

        public Sequence add(float value, short duration_ms) {
            this.steps.add(new Step(value, duration_ms));
            return this;
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
        short duration_ms;

        public Step(float value, short duration_ms) {
            this.value = value;
            this.duration_ms = duration_ms;
        }

        private void serializeTo(ByteBuffer buffer) {
            buffer.putFloat(this.value);
            buffer.putShort(this.duration_ms);
        }
    }
}
