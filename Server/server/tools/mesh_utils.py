def triangulate_faces(faces: list) -> list:
    """Convert n-gon faces to triangles using fan triangulation."""
    tris = []
    for face in faces:
        if len(face) == 3:
            tris.append(face)
        elif len(face) == 4:
            tris.append([face[0], face[1], face[2]])
            tris.append([face[0], face[2], face[3]])
        else:
            for i in range(1, len(face) - 1):
                tris.append([face[0], face[i], face[i + 1]])
    return tris
