package org.Azkerash.riftfall.client;

import yesman.epicfight.api.client.model.Meshes.MeshAccessor;
import yesman.epicfight.client.mesh.HumanoidMesh;

final class GoblinMeshes {
    static final MeshAccessor<HumanoidMesh> BODY=MeshAccessor.create("riftfall","entity/goblin",
        loader->loader.loadSkinnedMesh(HumanoidMesh::new));
    static final MeshAccessor<HumanoidMesh> SHAMAN=MeshAccessor.create("riftfall","entity/goblin_shaman",
        loader->loader.loadSkinnedMesh(HumanoidMesh::new));
    private GoblinMeshes() {}
}
