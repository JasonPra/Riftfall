#!/usr/bin/env python3
"""Package native JoCodes captured from planted Legacy trees; never author tree geometry.

The checked-in manifest records the seed, soil, growth pulses and branch radii for
review. Capture samples with /legacytree export after growing real seedlings,
then record their planted origin and growth conditions in the manifest before packaging.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[2]
manifest=json.loads((ROOT/'tools/trees/legacy-grown-samples.json').read_text())
assert manifest['species']=='riftfall:vermillion_legacy'
samples=manifest['samples']
assert len(samples)>=3
alphabet=set('ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/')
for sample in samples:
 assert sample['source']=='planted_seedling'
 assert sample['jo_code'] and set(sample['jo_code'])<=alphabet
 assert set(sample['roots_jo_code'])<=alphabet
 assert sample.get('growth_stage','adult')!='adult' or sample['roots_jo_code']
 assert sample['branch_reach']<=18, 'Capture exceeds the normal world-generation leaf-write bounds'
adults=[sample for sample in samples if sample.get("growth_stage","adult")=="adult"]
lines=[f"{radius}:{sample['jo_code']}" for radius in range(2,9) for sample in adults]
lines += [f"*{radius}:{sample['roots_jo_code']}" for radius in range(2,9) for sample in adults]
(ROOT/'src/main/resources/trees/riftfall/jo_codes/vermillion_legacy.txt').write_text('\n'.join(lines)+'\n')
profiles=[{'jo_code':sample['jo_code'],'roots_jo_code':sample['roots_jo_code'],'growth_stage':sample.get('growth_stage','adult'),'branches':sample['branches']} for sample in samples]
(ROOT/'src/main/resources/trees/riftfall/jo_codes/vermillion_legacy_profiles.json').write_text(json.dumps(profiles,separators=(',',':'))+'\n')
for sample in samples:print(f"Grown seedling: {sample['pulses']} pulses, {len(sample['branches'])} branches, reach {sample['branch_reach']}")
