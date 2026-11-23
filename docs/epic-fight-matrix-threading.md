# Epic Fight matrix scratch isolation

Epic Fight 21.17.3.1's `OpenMatrix4f` scalar translation and scale overloads write to the same static `VECTOR_STORAGE` object. Rendering and integrated-server collision run on separate threads. If a translation overwrites that vector after a scale call sets it but before multiplication consumes it, a valid scale can become a translation vector containing zero components. The resulting singular shield transform then reaches the dependency's null-destination inversion bug.

A deterministic interleaving reproduced a `.75, .75, .75` scale becoming singular: determinant `0.0` instead of `.421875`. The native verification repeated the overlap with the actual forearm shield correction, after transformation by Mixin, and retained `.421875`.

`EpicFightMatrixScratchMixin` redirects reads of the dependency's mutable vector, vector4, matrix and matrix-conversion buffer scratch to storage owned by the calling thread. It preserves the existing math and allocation reuse without locking the render and server threads together. It does not change shield size or position, replace invalid geometry with identity transforms, or suppress attack exceptions. The `ShieldBox` validity checks remain a final defense for genuinely malformed transforms.

The redirects are specific to the pinned Epic Fight build. Their required field-read counts are 16 vector, 2 vector4, 71 matrix and 7 buffer reads. Recheck the dependency's bytecode and repeat the native interleaving when upgrading it.

The warrior Step adapter now permits starting from a shield guard, explicitly ends item use before playing Step, and marks the dash as owning the action while it executes. Buckler use resumes only after the dash and Epic Fight's action state release it. This prevents a stale raised-shield state from surviving the transition.

Verification uses a forced two-thread scale/correction overlap plus live shield sampling before, during and after Step. Both vanilla shields and forearm bucklers, in both hands, must have invertible physical transforms and accept a segment crossing their center. The warrior must guard before Step, stop blocking on entry, and guard again afterward. Temporary native fixtures are excluded from the release jar.
