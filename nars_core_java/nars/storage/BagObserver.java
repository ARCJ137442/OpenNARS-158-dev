package nars.storage;

import nars.entity.Item;

/**
 * Bag Observer; similar to Observer design pattern, except that here we have a single observer
 */
public interface BagObserver<BagType extends Item> {

    /**
     * Set a name for this observer
     */
	public abstract void setTitle(String title);

    /**
     * Set the observed Bag
     */
	public abstract void setBag( Bag<BagType> concepts );

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
}