package sh.swisschili.chat.client;

/**
 * Used to implement auto-scrolling
 */
public interface ItemAddedListener {
    void beforeItemAdded();
    void afterItemAdded();
}
