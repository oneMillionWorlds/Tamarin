import bpy
import mathutils
import math

def calc_roll_from_quaternion(quaternion):

    mat = quaternion.to_matrix()    # corresponds to EditBone.matrix.to_3x3()
    vec_yo = mathutils.Vector([0,1,0])
    vec = mat @ vec_yo              # corresponds to EditBone.vector

    # Get the rotation matrix to match vec_yo with vec direction
    if vec.cross(vec_yo).length == 0 and vec.dot(vec_yo) < 0:
        # Correct the rotation matrix when the direction of vec is equal to -vec_yo
        rot = Matrix.Diagonal((-1,-1,1),)
    else:
        diff_angle = vec_yo.rotation_difference(vec)
        rot = vec_yo.rotation_difference(vec).to_matrix()

    mat_roll = rot.inverted() @ mat
    v, angle = mat_roll.to_quaternion().to_axis_angle()

    from numpy import sign
    angle *= sign(v.y)

    return angle

# dictionary with new head, tail, and quaternion
bone_positions = {
[[BONE_DATA]]
}

def mult(quat, vec):
    w, x, y, z = quat.w, quat.x, quat.y, quat.z
    vx, vy, vz = vec.x, vec.y, vec.z

    store_x = w * w * vx + 2 * y * w * vz - 2 * z * w * vy + x * x * vx + 2 * y * x * vy + 2 * z * x * vz - z * z * vx - y * y * vx
    store_y = 2 * x * y * vx + y * y * vy + 2 * z * y * vz + 2 * w * z * vx - z * z * vy + w * w * vy - 2 * x * w * vz - x * x * vy
    store_z = 2 * x * z * vx + 2 * y * z * vy + z * z * vz - 2 * w * y * vx - y * y * vz + 2 * w * x * vy - x * x * vz + w * w * vz

    return mathutils.Vector((store_x, store_y, store_z))

def get_local_y(rotation_quaternion):
    local_y = mathutils.Vector((0.0, 1.0, 0.0))
    global_y = mult(rotation_quaternion, local_y)
    return global_y.normalized()

# Assume that the active object is the armature
if bpy.context.object and bpy.context.object.type == 'ARMATURE':
    armature = bpy.context.object
    bpy.context.view_layer.objects.active = armature
    bpy.ops.object.mode_set(mode='EDIT')

    edit_bones = armature.data.edit_bones

    for bone_name, positions in bone_positions.items():
        print(bone_name)

        if bone_name in edit_bones:
            edit_bone = edit_bones[bone_name]
        else:
            edit_bone = edit_bones.new(bone_name)
            if "root" in edit_bones:
                edit_bone.parent = edit_bones["root"]

        edit_bone.head = positions["head"]
        quaternion = positions["quaternion"];
        localY = get_local_y(quaternion);
        edit_bone.tail = positions["head"] + (localY*0.02) # 0.02 is arbitrary, but its a good length to see in the scale of a hand

        edit_bone.roll = calc_roll_from_quaternion(quaternion)

    bpy.ops.object.mode_set(mode='OBJECT')
else:
    print("No active armature object selected.")