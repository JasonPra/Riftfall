#!/usr/bin/env python3
"""Read existing OGG recordings; generate jaw amplitude curves without modifying the audio."""
from pathlib import Path
import array,math,subprocess
root=Path(__file__).resolve().parents[1]
rows=[]
for name in ('laugh','scream','death'):
    path=root/'src/main/resources/assets/riftfall/sounds/entity/goblin'/f'{name}.ogg'
    pcm=array.array('f',subprocess.check_output(['ffmpeg','-v','error','-i',str(path),'-ac','1','-ar','8000','-f','f32le','-']))
    width=400 # one value per 50ms, interpolated by the renderer
    amplitudes=[math.sqrt(sum(x*x for x in pcm[i:i+width])/len(pcm[i:i+width])) for i in range(0,len(pcm),width)]
    peak=max(amplitudes) or 1
    values=[0]+[min(1,(v/peak)**.65) if v/peak>.055 else 0 for v in amplitudes]+[0]
    rows.append(f'    public static final VoiceEnvelope {name.upper()}=new VoiceEnvelope({len(pcm)/8000:.6f}F,\n        '+','.join(f'{v:.3f}F' for v in values)+');')
path=root/'src/main/java/org/Azkerash/riftfall/facial/GoblinVoiceEnvelopes.java'
path.write_text('package org.Azkerash.riftfall.facial;\n/** Generated from the original recordings by tools/generate_goblin_voice_envelopes.py. */\npublic final class GoblinVoiceEnvelopes {\n'+'\n'.join(rows)+'\n    private GoblinVoiceEnvelopes(){}\n}\n')
