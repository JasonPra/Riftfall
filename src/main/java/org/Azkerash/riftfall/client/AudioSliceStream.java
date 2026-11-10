package org.Azkerash.riftfall.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;

/** A frame-aligned PCM window over a decoded stream, without generating an audio asset. */
public final class AudioSliceStream implements AudioStream {
    private final AudioStream source;
    private final AudioFormat format;
    private final int frameSize;
    private final long totalFrames;
    private long skipBytes,remaining,playedFrames;
    private ByteBuffer buffered=ByteBuffer.allocate(0);
    public AudioSliceStream(AudioStream source,double startSeconds,double durationSeconds) {
        if(!Double.isFinite(startSeconds)||startSeconds<0||!Double.isFinite(durationSeconds)||durationSeconds<=0)
            throw new IllegalArgumentException("Invalid audio window");
        this.source=source;format=source.getFormat();frameSize=format.getFrameSize();
        if(frameSize<=0 || format.getFrameRate()<=0)throw new IllegalArgumentException("Unknown PCM frame format");
        skipBytes=Math.round(startSeconds*format.getFrameRate())*frameSize;
        totalFrames=Math.round(durationSeconds*format.getFrameRate());remaining=totalFrames*frameSize;
    }
    @Override public AudioFormat getFormat(){return format;}
    private boolean refill(int requested) throws IOException {
        if(buffered.hasRemaining())return true;
        buffered=source.read(Math.max(frameSize,requested));return buffered.hasRemaining();
    }
    @Override public ByteBuffer read(int size) throws IOException {
        while(skipBytes>0 && refill((int)Math.min(skipBytes,16384))) {
            int skip=(int)Math.min(skipBytes,buffered.remaining());
            buffered.position(buffered.position()+skip);skipBytes-=skip;
        }
        int count=(int)Math.min(Math.max(0,size),remaining);count-=count%frameSize;
        var output=ByteBuffer.allocateDirect(count).order(format.isBigEndian()?ByteOrder.BIG_ENDIAN:ByteOrder.LITTLE_ENDIAN);
        if(skipBytes>0)return output.limit(0);
        while(output.hasRemaining() && refill(Math.min(output.remaining(),16384))) {
            int take=Math.min(output.remaining(),buffered.remaining());
            var part=buffered.duplicate();part.limit(part.position()+take);output.put(part);
            buffered.position(buffered.position()+take);remaining-=take;
        }
        output.flip();
        // Short fades avoid clicks at arbitrary waveform cuts. Vanilla Vorbis decodes to signed 16-bit PCM.
        if(format.getSampleSizeInBits()==16 && format.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            for(int frame=0;frame<output.remaining()/frameSize;frame++) {
                long index=playedFrames+frame;
                float gain=(float)Math.max(0,Math.min(1,Math.min(index/(format.getFrameRate()*.01),
                    (totalFrames-1-index)/(format.getFrameRate()*.015))));
                for(int channel=0;channel<format.getChannels();channel++) {
                    int offset=frame*frameSize+channel*2;output.putShort(offset,(short)(output.getShort(offset)*gain));
                }
            }
        }
        playedFrames+=output.remaining()/frameSize;
        return output;
    }
    @Override public void close() throws IOException {source.close();}
}
