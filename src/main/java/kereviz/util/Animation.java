package kereviz.util;

public interface Animation {
    enum Direction {
        FORWARDS,
        BACKWARDS
    }

    void reset();
    void setDirection(Direction direction);
    boolean isDone();
    double getOutput();
}