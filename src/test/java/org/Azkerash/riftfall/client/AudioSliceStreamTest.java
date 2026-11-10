package org.Azkerash.riftfall.client;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioSliceStreamTest {
    private static final class Pcm implements AudioStream {
        final AudioFormat format;final ByteBuffer data;boolean closed;
        Pcm(int frames,int channels) {
            format=new AudioFormat(1000,16,channels,true,false);
            data=ByteBuffer.allocate(frames*channels*2).order(ByteOrder.LITTLE_ENDIAN);
            for(int i=0;i<frames;i++)for(int c=0;c<channels;c++)data.putShort((short)((1000+i)*(c==0?1:-1)));
            data.flip();
        }
        public AudioFormat getFormat(){return format;}
        public ByteBuffer read(int requested) {
            // Deliberately return more than requested: the window must preserve the unused PCM.
            int take=Math.min(data.remaining(),4096);var out=data.slice();out.limit(take);data.position(data.position()+take);return out;
        }
        public void close(){closed=true;}
    }
    private ByteBuffer all(AudioSliceStream stream) throws Exception {
        var bytes=new ByteArrayOutputStream();
        while(true){var part=stream.read(204);if(!part.hasRemaining())break;while(part.hasRemaining())bytes.write(part.get());}
        return ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN);
    }
    @Test void cropsTheRequestedHalfSecondEvenWhenDecoderReadsPastTheWindow() throws Exception {
        var pcm=new Pcm(4000,1);
        try(var slice=new AudioSliceStream(pcm,1,.5)) {
            var result=all(slice);assertEquals(1000,result.remaining());
            assertEquals(0,result.getShort(0));assertEquals(2200,result.getShort(200*2));
            assertEquals(0,result.getShort(998));assertEquals(0,slice.read(4096).remaining());
        }
        assertTrue(pcm.closed);
    }
    @Test void keepsStereoFramesAlignedAndFadesBothChannelsTogether() throws Exception {
        try(var slice=new AudioSliceStream(new Pcm(1000,2),.1,.1)) {
            var result=all(slice);assertEquals(400,result.remaining());
            assertEquals(1150,result.getShort(50*4));assertEquals(-1150,result.getShort(50*4+2));
            assertEquals(0,result.getShort(0));assertEquals(0,result.getShort(396));assertEquals(0,result.getShort(398));
        }
    }
    @Test void returnsEofForMissingSourceFramesAndRejectsInvalidWindows() throws Exception {
        try(var slice=new AudioSliceStream(new Pcm(100,1),1,.5)){assertEquals(0,slice.read(4096).remaining());}
        assertThrows(IllegalArgumentException.class,()->new AudioSliceStream(new Pcm(100,1),-.1,.5));
        assertThrows(IllegalArgumentException.class,()->new AudioSliceStream(new Pcm(100,1),0,Double.NaN));
    }
}
