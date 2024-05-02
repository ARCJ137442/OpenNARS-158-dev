package nars.io;

/**
 * An interface to be implemented in all input channels
 * to get the input for the next moment from an input channel
 */
public interface InputChannel {
	/** @return value indicating whether the reasoner should run */
	public boolean nextInput();
}
