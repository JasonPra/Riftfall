import unittest
from crystal_surfaces import exterior_faces

SIDES=('west','east','down','up','north','south')
def box(a,b,part=0,shell=True):
 return {'from':a,'to':b,'shade':shell,'faces':{s:{'texture':'#crystal' if shell else '#core','tintindex':part,'uv':[0,0,16,16]} for s in SIDES}}
def area(elements):
 total=0
 for e in elements:
  spans=[b-a for a,b in zip(e['from'],e['to'])]
  total+=max(spans[0]*spans[1],spans[0]*spans[2],spans[1]*spans[2])
 return total

class CrystalSurfacesTest(unittest.TestCase):
 def test_overlapping_prisms_keep_the_complete_outside_and_no_buried_caps(self):
  a=box([0,0,0],[2,1,1]);b=box([1,0,0],[3,1,1],1)
  for inputs in ([a,b],[b,a]):
   result=exterior_faces(inputs)
   self.assertAlmostEqual(14,area(result))
   for e in result:
    side=next(iter(e['faces']))
    if side in ('west','east'):self.assertIn(e['from'][0],(0,3))
 def test_touching_prisms_remove_both_sides_of_the_internal_join(self):
  result=exterior_faces([box([0,0,0],[1,1,1]),box([1,0,0],[2,1,1],1)])
  self.assertAlmostEqual(10,area(result))
  self.assertFalse(any(e['from'][0]==e['to'][0]==1 for e in result))
 def test_inner_glowing_core_is_not_erased_by_outer_shell(self):
  result=exterior_faces([box([0,0,0],[2,2,2]),box([.5,.5,.5],[1.5,1.5,1.5],1,False)])
  self.assertAlmostEqual(24,area([e for e in result if e['shade']]))
  self.assertAlmostEqual(6,area([e for e in result if not e['shade']]))
 def test_visible_fragments_keep_their_original_texture_scale(self):
  result=exterior_faces([box([0,0,0],[2,1,1]),box([1,.5,0],[3,2,1],1)])
  top=[e for e in result if 'up' in e['faces'] and e['faces']['up']['tintindex']==0]
  self.assertEqual(1,len(top));self.assertEqual([0,0,8,16],top[0]['faces']['up']['uv'])

if __name__=='__main__':unittest.main()
