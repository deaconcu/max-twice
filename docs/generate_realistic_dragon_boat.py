#!/usr/bin/env python3
"""高精度真实龙舟 3D 模型生成器"""
import math

class RealisticDragonBoat:
    def __init__(self):
        self.vertices = []
        self.normals = []
        self.faces = []

    def add_vertex(self, x, y, z):
        self.vertices.append((x, y, z))
        return len(self.vertices)

    def add_normal(self, x, y, z):
        length = math.sqrt(x*x + y*y + z*z)
        if length > 0:
            self.normals.append((x/length, y/length, z/length))
        else:
            self.normals.append((0, 1, 0))
        return len(self.normals)

    def add_face(self, *indices):
        self.faces.append(indices)

    def create_hull(self):
        """创建船身 - 高精度平滑曲面"""
        length = 24.0
        width = 2.2
        height = 1.8
        z_segments = 40  # 纵向分段
        radial_segments = 16  # 径向分段

        for i in range(z_segments + 1):
            t = i / z_segments
            z = -length/2 + length * t

            # 船头船尾形状
            width_factor = 1.0 - (2*t - 1)**4 * 0.8
            height_factor = 1.0 - (2*t - 1)**2 * 0.3

            # 船头船尾上翘
            y_lift = (abs(2*t - 1)**2) * 1.2

            current_width = width * width_factor
            current_height = height * height_factor

            # 生成横截面圆形顶点
            for j in range(radial_segments):
                angle = (j / radial_segments) * math.pi

                # 船底扁平，船舷圆润
                if angle < math.pi * 0.3:
                    r_factor = 0.3
                elif angle > math.pi * 0.7:
                    r_factor = 0.3
                else:
                    r_factor = 1.0

                x = current_width * math.cos(angle)
                y = -current_height * 0.3 + current_height * math.sin(angle) * r_factor + y_lift

                self.add_vertex(x, y, z)

                # 计算法向量
                nx = math.cos(angle)
                ny = math.sin(angle) * r_factor
                self.add_normal(nx, ny, 0)

        # 连接面
        for i in range(z_segments):
            for j in range(radial_segments):
                v1 = i * radial_segments + j + 1
                v2 = i * radial_segments + (j + 1) % radial_segments + 1
                v3 = (i + 1) * radial_segments + (j + 1) % radial_segments + 1
                v4 = (i + 1) * radial_segments + j + 1

                self.add_face(v1, v2, v3, v4)

        # 封闭船头船尾
        front_center = self.add_vertex(0, 0.8, -length/2)
        back_center = self.add_vertex(0, 0.8, length/2)

        self.add_normal(0, 0, -1)
        self.add_normal(0, 0, 1)

        for j in range(radial_segments):
            v1 = j + 1
            v2 = (j + 1) % radial_segments + 1
            self.add_face(front_center, v2, v1)

            v3 = z_segments * radial_segments + j + 1
            v4 = z_segments * radial_segments + (j + 1) % radial_segments + 1
            self.add_face(back_center, v3, v4)

    def create_dragon_head(self):
        """创建精细龙头"""
        base_idx = len(self.vertices)
        z_pos = -13.0

        # 龙头基座
        head_base = [
            (0, 1.0, z_pos),
            (-0.6, 0.8, z_pos),
            (0.6, 0.8, z_pos),
            (-0.6, 0.4, z_pos),
            (0.6, 0.4, z_pos),
        ]

        # 龙嘴
        mouth = [
            (0, 1.2, z_pos - 1.8),  # 上嘴尖
            (0, 0.6, z_pos - 1.8),  # 下嘴尖
            (-0.5, 1.0, z_pos - 1.2),
            (0.5, 1.0, z_pos - 1.2),
            (-0.4, 0.8, z_pos - 1.0),
            (0.4, 0.8, z_pos - 1.0),
        ]

        # 龙眼
        for side in [-1, 1]:
            eye_x = side * 0.4
            for i in range(8):
                angle = i * math.pi * 2 / 8
                ex = eye_x + math.cos(angle) * 0.15
                ey = 1.0 + math.sin(angle) * 0.15
                self.add_vertex(ex, ey, z_pos - 0.6)
                self.add_normal(math.cos(angle), math.sin(angle), 0)

        # 龙角
        horn_segments = 8
        for side in [-1, 1]:
            for i in range(horn_segments):
                t = i / (horn_segments - 1)
                hx = side * (0.7 + t * 0.3)
                hy = 1.2 + t * 1.5 - t*t * 0.5
                hz = z_pos - 0.3 + t * 0.4
                radius = 0.12 * (1 - t * 0.7)

                for j in range(6):
                    angle = j * math.pi * 2 / 6
                    x = hx + math.cos(angle) * radius
                    y = hy + math.sin(angle) * radius
                    self.add_vertex(x, y, hz)
                    self.add_normal(math.cos(angle), math.sin(angle), 0)

        # 龙须
        for side in [-1, 1]:
            for i in range(5):
                t = i / 4
                wx = side * (0.3 + t * 0.4)
                wy = 0.8 - t * 0.6
                wz = z_pos - 1.0 - t * 0.8
                self.add_vertex(wx, wy, wz)

        # 添加所有基座和嘴部顶点
        for v in head_base + mouth:
            self.add_vertex(*v)
            self.add_normal(0, 0, -1)

    def create_dragon_tail(self):
        """创建精细龙尾"""
        z_pos = 13.0
        segments = 12

        for i in range(segments):
            t = i / (segments - 1)
            z = z_pos + t * 2.5
            y = 1.0 + t * 2.0 - t*t * 0.8
            width = 0.6 * (1 - t * 0.5)
            height = 0.4 * (1 - t * 0.3)

            # 尾部扇形
            for j in range(8):
                angle = (j / 7 - 0.5) * math.pi * 0.6
                x = width * math.sin(angle)
                y_offset = height * math.cos(angle)
                self.add_vertex(x, y + y_offset, z)
                self.add_normal(math.sin(angle), math.cos(angle), 0)

        # 连接尾部面
        for i in range(segments - 1):
            for j in range(7):
                base = len(self.vertices) - segments * 8 + i * 8 + j + 1
                self.add_face(base, base + 1, base + 9, base + 8)

    def create_seats_and_paddles(self):
        """创建座位和桨架"""
        num_seats = 12
        for i in range(num_seats):
            z = -9.0 + i * 1.5

            # 座板
            for side in [-1, 1]:
                x_base = side * 0.6
                self.add_vertex(x_base - 0.3, 0.3, z - 0.2)
                self.add_vertex(x_base + 0.3, 0.3, z - 0.2)
                self.add_vertex(x_base + 0.3, 0.3, z + 0.2)
                self.add_vertex(x_base - 0.3, 0.3, z + 0.2)

                self.add_vertex(x_base - 0.3, 0.2, z - 0.2)
                self.add_vertex(x_base + 0.3, 0.2, z - 0.2)
                self.add_vertex(x_base + 0.3, 0.2, z + 0.2)
                self.add_vertex(x_base - 0.3, 0.2, z + 0.2)

                base = len(self.vertices) - 7
                self.add_face(base, base+1, base+2, base+3)
                self.add_face(base+4, base+7, base+6, base+5)
                self.add_face(base, base+4, base+5, base+1)
                self.add_face(base+1, base+5, base+6, base+2)
                self.add_face(base+2, base+6, base+7, base+3)
                self.add_face(base+3, base+7, base+4, base)

    def create_decorations(self):
        """创建装饰纹理和龙鳞"""
        # 船身龙鳞纹理
        for i in range(20):
            z = -10.0 + i * 1.0
            for j in range(8):
                x = (j - 4) * 0.4
                if abs(x) > 0.5:
                    y = 0.5 + abs(x) * 0.3

                    # 鳞片
                    for k in range(6):
                        angle = k * math.pi * 2 / 6
                        sx = x + math.cos(angle) * 0.08
                        sy = y + math.sin(angle) * 0.08
                        self.add_vertex(sx, sy, z)

    def export_obj(self, filename):
        """导出 OBJ 文件"""
        with open(filename, 'w') as f:
            f.write("# Realistic Dragon Boat Model\n")
            f.write(f"# Vertices: {len(self.vertices)}\n")
            f.write(f"# Faces: {len(self.faces)}\n\n")

            for v in self.vertices:
                f.write(f"v {v[0]:.6f} {v[1]:.6f} {v[2]:.6f}\n")

            f.write("\n")
            for n in self.normals:
                f.write(f"vn {n[0]:.6f} {n[1]:.6f} {n[2]:.6f}\n")

            f.write("\n")
            for face in self.faces:
                f.write("f " + " ".join(f"{idx}//{idx}" for idx in face) + "\n")

        print(f"✓ 生成真实龙舟模型")
        print(f"  顶点: {len(self.vertices)}")
        print(f"  面: {len(self.faces)}")
        print(f"  文件: {filename}")

if __name__ == "__main__":
    boat = RealisticDragonBoat()
    print("生成船身...")
    boat.create_hull()
    print("生成龙头...")
    boat.create_dragon_head()
    print("生成龙尾...")
    boat.create_dragon_tail()
    print("生成座位...")
    boat.create_seats_and_paddles()
    print("生成装饰...")
    boat.create_decorations()
    boat.export_obj("dragon_boat_realistic.obj")
