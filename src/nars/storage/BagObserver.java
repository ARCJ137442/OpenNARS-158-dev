package nars.storage;

import nars.entity.Item;

/**
 * Bag Observer; similar to Observer design pattern, except that here we have a
 * single observer
 */
public interface BagObserver<BagType extends Item> {

    /**
     * Set a name for this observer
     */
    public abstract void setTitle(String title);

    /**
     * Set the observed Bag
     */
    public abstract void setBag(Bag<BagType> concepts);

    /**
     * Post given bag content
     *
     * @param str The text
     */
    public abstract void post(String str);

    /**
     * Refresh display if in showing state
     */
    public abstract void refresh(String string);

    /**
     * put in non-showing state
     */
    public abstract void stop();

    /** a {@link BagObserver} that does nothing (null design pattern) */
    public class NullObserver<BagType extends Item> implements BagObserver<BagType> {
        @Override
        public void setTitle(String title) {
        }

        @Override
        public void setBag(Bag<BagType> concepts) {
        }

        @Override
        public void post(String str) {
        }

        @Override
        public void refresh(String string) {
        }

        @Override
        public void stop() {
        }
    }
}