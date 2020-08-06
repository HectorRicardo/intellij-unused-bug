package impl;

import gervill.javax.sound.sampled.AudioFormat;
import gervill.javax.sound.sampled.Control;
import gervill.javax.sound.sampled.Line;
import gervill.javax.sound.sampled.LineListener;
import gervill.javax.sound.sampled.LineUnavailableException;
import gervill.javax.sound.sampled.SourceDataLine;

public class SourceDataLineImpl implements SourceDataLine {
	
	private final javax.sound.sampled.SourceDataLine realLine;
	
	private static javax.sound.sampled.AudioFormat convertToOriginalFormat(AudioFormat format) {
    	javax.sound.sampled.AudioFormat.Encoding encoding = new javax.sound.sampled.AudioFormat.Encoding(format.getEncoding().toString());
    	return new javax.sound.sampled.AudioFormat(encoding, format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), format.properties());
    }
    
    private static AudioFormat convertToNewFormat(javax.sound.sampled.AudioFormat format) {
    	AudioFormat.Encoding encoding = new AudioFormat.Encoding(format.getEncoding().toString());
    	return new AudioFormat(encoding, format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.getFrameSize(), format.getFrameRate(), format.isBigEndian(), format.properties());
    }

	public SourceDataLineImpl(AudioFormat format) throws LineUnavailableException {
		try {
			realLine = javax.sound.sampled.AudioSystem.getSourceDataLine(convertToOriginalFormat(format));
		} catch (javax.sound.sampled.LineUnavailableException e) {
			throw new LineUnavailableException(e.getMessage());
    	}
	}
	
	@Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
    	try {
    		realLine.open(convertToOriginalFormat(format), bufferSize);
    	} catch (javax.sound.sampled.LineUnavailableException e) {
    		throw new LineUnavailableException(e.getMessage());
    	}
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
    	try {
    		realLine.open(convertToOriginalFormat(format));
    	} catch (javax.sound.sampled.LineUnavailableException e) {
    		throw new LineUnavailableException(e.getMessage());
    	}
    }

    @Override
    public int write(byte[] b, int off, int len) {
        return realLine.write(b, off, len);
    }

    @Override
    public void drain() {
    	realLine.drain();
    }

    @Override
    public void flush() {
    	realLine.flush();
    }

    @Override
    public void start() {
    	realLine.start();
    }

    @Override
    public void stop() {
    	realLine.stop();
    }

    @Override
    public boolean isRunning() {
        return realLine.isRunning();
    }

    @Override
    public boolean isActive() {
        return realLine.isActive();
    }

    @Override
    public AudioFormat getFormat() {
        return convertToNewFormat(realLine.getFormat());
    }

    @Override
    public int getBufferSize() {
        return realLine.getBufferSize();
    }

    @Override
    public int available() {
        return realLine.available();
    }

    @Override
    public int getFramePosition() {
        return realLine.getFramePosition();
    }

    @Override
    public long getLongFramePosition() {
        return realLine.getLongFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return realLine.getMicrosecondPosition();
    }

    @Override
    public float getLevel() {
        return realLine.getLevel();
    }

    @Override
    public Line.Info getLineInfo() {
        return new Line.Info(realLine.getLineInfo().getLineClass());
    }

    @Override
    public void open() throws LineUnavailableException {
    	try {
    		realLine.open();
    	} catch (javax.sound.sampled.LineUnavailableException e) {
    		throw new LineUnavailableException(e.getMessage());
    	}
    }

    @Override
    public void close() {
    	realLine.close();
    }

    @Override
    public boolean isOpen() {
        return realLine.isOpen();
    }

    @Override
    public Control[] getControls() {
        return new Control[0];
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return false;
    }

    @Override
    public Control getControl(Control.Type control) {
        return null;
    }

    @Override
    public void addLineListener(LineListener listener) {
    	// realLine.addLineListener(listener);
    }

    @Override
    public void removeLineListener(LineListener listener) {
    	// realLine.removeLineListener(listener);
    }
}