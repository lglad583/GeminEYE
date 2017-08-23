package vision.gemineye.framework;

public abstract class TaskBlocks<T> implements Task {

    private T[] blocks;

    public int length() {
        return blocks.length;
    }

    public TaskBlocks(T... blocks) {
        this.blocks = blocks;
    }

    public abstract String resolveFor(T block) throws Exception;

    public String resolve(int index) throws Exception {
        return resolveFor(blocks[index]);

    }
}
