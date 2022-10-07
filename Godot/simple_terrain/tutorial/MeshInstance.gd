extends MeshInstance

# Called when the node enters the scene tree for the first time.
func _ready():
	mesh.material.set_shader_param("height_scale", 0.5)

# Called every frame. 'delta' is the elapsed time since the previous frame.
#func _process(delta):
#	pass
