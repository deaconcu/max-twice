#!/usr/bin/env python3
"""
低多边形龙舟 3D 模型生成器
生成传统中国龙舟的 OBJ 格式文件
"""

import math

class DragonBoatGenerator:
    def __init__(self):
        self.vertices = []
        self.faces = []
        self.vertex_index = 1  # OBJ format starts from 1

    def add_vertex(self, x, y, z):
        """添加顶点"""
        self.vertices.append((x, y, z))
        return self.vertex_index

    def add_face(self, *indices):
        """添加面（三角形或四边形）"""
        self.faces.append(indices)

    def create_boat_hull(self):
        """创建船身主体 - 修长的龙舟造型"""
        length = 20.0  # 船长
        width = 2.0    # 船宽
        height = 1.5   # 船高
        segments = 12  # 分段数

        # 船身截面 - 从船头到船尾
        for i in range(segments + 1):
            t = i / segments
            z = -length/2 + length * t

            # 船头船尾变窄
            width_scale = 1.0 - abs(2*t - 1)**2 * 0.7
            current_width = width * width_scale

            # 船头船尾翘起
            y_offset = abs(2*t - 1)**1.5 * 0.8

            # 船底和船舷的顶点
            # 底部中心
            self.add_vertex(0, -height/2 + y_offset, z)

            # 底部左右
            self.add_vertex(-current_width/2, -height/3 + y_offset, z)
            self.add_vertex(current_width/2, -height/3 + y_offset, z)

            # 船舷左右
            self.add_vertex(-current_width/2 * 1.1, height/2 + y_offset, z)
            self.add_vertex(current_width/2 * 1.1, height/2 + y_offset, z)

        # 连接面
        for i in range(segments):
            base = i * 5 + 1
            next_base = (i + 1) * 5 + 1

            # 左侧面
            self.add_face(base, base+1, next_base+1, next_base)
            self.add_face(base+1, base+3, next_base+3, next_base+1)

            # 右侧面
            self.add_face(base, next_base, next_base+2, base+2)
            self.add_face(base+2, next_base+2, next_base+4, base+4)

            # 底部
            self.add_face(base, base+2, base+1)

        # 封闭船头
        front_base = 1
        self.add_face(front_base, front_base+1, front_base+3)
        self.add_face(front_base, front_base+3, front_base+4)
        self.add_face(front_base, front_base+4, front_base+2)

        # 封闭船尾
        back_base = segments * 5 + 1
        self.add_face(back_base, back_base+3, back_base+1)
        self.add_face(back_base, back_base+4, back_base+3)
        self.add_face(back_base, back_base+2, back_base+4)

        return segments * 5 + 5

    def create_dragon_head(self, offset_z):
        """创建龙头装饰"""
        base_idx = len(self.vertices) + 1

        # 龙头基础位置
        x_base = 0
        y_base = 1.0
        z_base = offset_z - 1.5

        # 龙嘴（尖锐的低多边形造型）
        self.add_vertex(x_base, y_base + 0.3, z_base - 1.2)  # 上嘴尖
        self.add_vertex(x_base, y_base - 0.3, z_base - 1.2)  # 下嘴尖

        # 龙头侧面
        self.add_vertex(x_base - 0.4, y_base, z_base - 0.8)  # 左
        self.add_vertex(x_base + 0.4, y_base, z_base - 0.8)  # 右

        # 龙头后部
        self.add_vertex(x_base - 0.5, y_base + 0.5, z_base)
        self.add_vertex(x_base + 0.5, y_base + 0.5, z_base)
        self.add_vertex(x_base - 0.5, y_base - 0.5, z_base)
        self.add_vertex(x_base + 0.5, y_base - 0.5, z_base)

        # 龙角
        self.add_vertex(x_base - 0.6, y_base + 1.2, z_base - 0.3)
        self.add_vertex(x_base + 0.6, y_base + 1.2, z_base - 0.3)

        # 连接面
        # 上嘴
        self.add_face(base_idx, base_idx+2, base_idx+4)
        self.add_face(base_idx, base_idx+4, base_idx+5)
        self.add_face(base_idx, base_idx+5, base_idx+3)
        self.add_face(base_idx, base_idx+3, base_idx+2)

        # 下嘴
        self.add_face(base_idx+1, base_idx+6, base_idx+2)
        self.add_face(base_idx+1, base_idx+7, base_idx+6)
        self.add_face(base_idx+1, base_idx+3, base_idx+7)
        self.add_face(base_idx+1, base_idx+2, base_idx+3)

        # 龙头侧面
        self.add_face(base_idx+2, base_idx+6, base_idx+4)
        self.add_face(base_idx+3, base_idx+5, base_idx+7)

        # 后面
        self.add_face(base_idx+4, base_idx+6, base_idx+7, base_idx+5)

        # 左龙角
        self.add_face(base_idx+4, base_idx+8, base_idx+2)
        self.add_face(base_idx+4, base_idx+5, base_idx+8)

        # 右龙角
        self.add_face(base_idx+5, base_idx+3, base_idx+9)
        self.add_face(base_idx+5, base_idx+9, base_idx+8)

    def create_dragon_tail(self, offset_z):
        """创建龙尾装饰"""
        base_idx = len(self.vertices) + 1

        x_base = 0
        y_base = 1.0
        z_base = offset_z + 1.5

        # 龙尾基座
        self.add_vertex(x_base - 0.4, y_base + 0.3, z_base)
        self.add_vertex(x_base + 0.4, y_base + 0.3, z_base)
        self.add_vertex(x_base - 0.4, y_base - 0.3, z_base)
        self.add_vertex(x_base + 0.4, y_base - 0.3, z_base)

        # 龙尾中段
        self.add_vertex(x_base - 0.5, y_base + 0.6, z_base + 0.5)
        self.add_vertex(x_base + 0.5, y_base + 0.6, z_base + 0.5)
        self.add_vertex(x_base, y_base - 0.4, z_base + 0.5)

        # 龙尾尖端（向上翘）
        self.add_vertex(x_base, y_base + 1.5, z_base + 1.2)

        # 连接面
        # 基座到中段
        self.add_face(base_idx, base_idx+4, base_idx+5, base_idx+1)
        self.add_face(base_idx, base_idx+2, base_idx+6, base_idx+4)
        self.add_face(base_idx+1, base_idx+5, base_idx+6, base_idx+3)
        self.add_face(base_idx+2, base_idx+3, base_idx+6)

        # 中段到尖端
        self.add_face(base_idx+4, base_idx+7, base_idx+5)
        self.add_face(base_idx+4, base_idx+6, base_idx+7)
        self.add_face(base_idx+5, base_idx+7, base_idx+6)

    def create_seats(self, count=10):
        """创建座位标记（简单的横杆）"""
        length = 18.0
        start_z = -length/2 + 2
        end_z = length/2 - 2
        spacing = (end_z - start_z) / (count - 1)

        for i in range(count):
            z = start_z + i * spacing
            base_idx = len(self.vertices) + 1

            # 横杆
            y = 0.2
            width = 1.6
            thickness = 0.1

            # 8个顶点构成横杆
            self.add_vertex(-width/2, y - thickness, z - 0.1)
            self.add_vertex(width/2, y - thickness, z - 0.1)
            self.add_vertex(width/2, y - thickness, z + 0.1)
            self.add_vertex(-width/2, y - thickness, z + 0.1)

            self.add_vertex(-width/2, y + thickness, z - 0.1)
            self.add_vertex(width/2, y + thickness, z - 0.1)
            self.add_vertex(width/2, y + thickness, z + 0.1)
            self.add_vertex(-width/2, y + thickness, z + 0.1)

            # 6个面
            self.add_face(base_idx, base_idx+1, base_idx+2, base_idx+3)  # 底
            self.add_face(base_idx+4, base_idx+7, base_idx+6, base_idx+5)  # 顶
            self.add_face(base_idx, base_idx+4, base_idx+5, base_idx+1)  # 前
            self.add_face(base_idx+2, base_idx+6, base_idx+7, base_idx+3)  # 后
            self.add_face(base_idx, base_idx+3, base_idx+7, base_idx+4)  # 左
            self.add_face(base_idx+1, base_idx+5, base_idx+6, base_idx+2)  # 右

    def generate(self):
        """生成完整的龙舟模型"""
        print("正在生成船身...")
        self.create_boat_hull()

        print("正在生成龙头...")
        self.create_dragon_head(-10.0)

        print("正在生成龙尾...")
        self.create_dragon_tail(10.0)

        print("正在生成座位...")
        self.create_seats(12)

        self.vertex_index = len(self.vertices) + 1

    def export_obj(self, filename):
        """导出为 OBJ 格式"""
        print(f"正在导出到 {filename}...")

        with open(filename, 'w') as f:
            f.write("# Low Poly Dragon Boat Model\n")
            f.write("# Generated by DragonBoatGenerator\n")
            f.write(f"# Vertices: {len(self.vertices)}\n")
            f.write(f"# Faces: {len(self.faces)}\n\n")

            # 写入顶点
            f.write("# Vertices\n")
            for v in self.vertices:
                f.write(f"v {v[0]:.6f} {v[1]:.6f} {v[2]:.6f}\n")

            f.write("\n# Faces\n")
            # 写入面
            for face in self.faces:
                f.write("f " + " ".join(str(idx) for idx in face) + "\n")

        print(f"✓ 成功生成龙舟模型！")
        print(f"  顶点数: {len(self.vertices)}")
        print(f"  面数: {len(self.faces)}")
        print(f"  文件: {filename}")

if __name__ == "__main__":
    generator = DragonBoatGenerator()
    generator.generate()
    generator.export_obj("dragon_boat_lowpoly.obj")
