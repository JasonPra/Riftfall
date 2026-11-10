# Third-person camera

The rear third-person camera uses a 4.5-unit distance, a 0.35-unit right shoulder offset and a 0.15-unit height offset. Change these in `config/riftfall-camera-client.toml`; the enhancement can also be disabled there. Front third-person, first-person and Epic Fight’s aiming zoom retain their existing camera behavior.

The camera checks the center and four near-plane corners against terrain. It retracts immediately at a wall and extends smoothly when the obstruction clears. Rotation and normal aiming remain under Minecraft/Epic Fight control.

During a backstab, Epic Fight lock-on and its selected target are cleared. The attacker cannot move, jump or turn their character, but mouse movement can orbit the camera independently. Lock-on stays off after the scene. A first-person victim temporarily switches to rear third-person until recovery; if killed, that view remains through the death scene and restores after respawn. A manually selected alternative perspective is respected when restoring.
