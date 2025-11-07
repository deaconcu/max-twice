#!/usr/bin/env python3
"""
生成精确的双层龙舟模型
确保所有面片索引正确
"""

import math

class DragonBoatGenerator:
    def __init__(self):
        self.vertices = []
        self.faces = []
        self.vertex_count = 0

    def add_vertex(self, x, y, z):
        """添加顶点"""
        self.vertices.append(f"v {x:.3f} {y:.3f} {z:.3f}")
        self.vertex_count += 1
        return self.vertex_count

    def add_face(self, *indices):
        """添加面片（支持三角形或四边形）"""
        face_str = "f " + " ".join(str(i) for i in indices)
        self.faces.append(face_str)

    def generate_hull_outer(self):
        """生成船体外壁顶点"""
        print("生成船体外壁...")
        segments = 20  # 纵向分段

        outer_indices = []

        for i in range(segments):
            z = -10.0 + i * 1.0  # z 从 -10 到 9
            t = i / (segments - 1)  # 0 到 1

            # 计算船体宽度（中间最宽）
            width_factor = 1.0 - abs(t - 0.5) * 0.6

            # 计算船底深度（船头船尾上翘）
            depth_factor = 1.0 - abs(t - 0.5) * 0.3

            # 每段9个顶点：中线、左3、右3、左船舷、右船舷
            segment_indices = []

            # 1. 船底中线
            y_bottom = -0.76 * depth_factor
            idx = self.add_vertex(0.0, y_bottom, z)
            segment_indices.append(idx)

            # 2-4. 左侧船底（3个点）
            left_widths = [0.82, 1.22, 1.52]
            for w in left_widths:
                w_scaled = -w * width_factor
                y = y_bottom + 0.05 * abs(w / 1.52)
                idx = self.add_vertex(w_scaled, y, z)
                segment_indices.append(idx)

            # 5. 左船舷
            y_side = 0.05
            idx = self.add_vertex(-1.7 * width_factor, y_side, z)
            segment_indices.append(idx)

            # 6-8. 右侧船底（3个点）
            for w in left_widths:
                w_scaled = w * width_factor
                y = y_bottom + 0.05 * abs(w / 1.52)
                idx = self.add_vertex(w_scaled, y, z)
                segment_indices.append(idx)

            # 9. 右船舷
            idx = self.add_vertex(1.7 * width_factor, y_side, z)
            segment_indices.append(idx)

            outer_indices.append(segment_indices)

        return outer_indices

    def generate_hull_inner(self, outer_indices):
        """生成船体内壁顶点（基于外壁内缩）"""
        print("生成船体内壁...")
        inner_indices = []
        thickness = 0.12

        for segment in outer_indices:
            inner_segment = []
            # 跳过船头和船尾（只生成中间18段的内壁）
            seg_idx = outer_indices.index(segment)
            if seg_idx == 0 or seg_idx == len(outer_indices) - 1:
                inner_segment = [None] * 9
                inner_indices.append(inner_segment)
                continue

            z_offset = 0.15  # 内壁 z 向后偏移

            for i, outer_idx in enumerate(segment):
                # 获取外壁顶点坐标
                outer_v = self.vertices[outer_idx - 1].split()[1:]
                x, y, z = float(outer_v[0]), float(outer_v[1]), float(outer_v[2])

                # 计算内壁坐标（向内缩进）
                if i == 0:  # 中线
                    x_inner = x
                    y_inner = y + thickness
                elif i < 5:  # 左侧
                    x_inner = x + thickness
                    y_inner = y + thickness * 0.5
                else:  # 右侧
                    x_inner = x - thickness
                    y_inner = y + thickness * 0.5

                z_inner = z + z_offset

                idx = self.add_vertex(x_inner, y_inner, z_inner)
                inner_segment.append(idx)

            inner_indices.append(inner_segment)

        return inner_indices

    def generate_hull_faces_outer(self, outer_indices):
        """生成船体外壁面片"""
        print("生成外壁面片...")

        for i in range(len(outer_indices) - 1):
            curr = outer_indices[i]
            next_seg = outer_indices[i + 1]

            # 左侧外壁（0-1-2-3-4）
            for j in range(4):
                v1 = curr[j]
                v2 = curr[j + 1]
                v3 = next_seg[j + 1]
                v4 = next_seg[j]
                self.add_face(v1, v2, v3, v4)

            # 右侧外壁（0-5-6-7-8）
            indices = [0, 5, 6, 7, 8]
            for j in range(4):
                v1 = curr[indices[j]]
                v2 = curr[indices[j + 1]]
                v3 = next_seg[indices[j + 1]]
                v4 = next_seg[indices[j]]
                self.add_face(v1, v2, v3, v4)

    def generate_hull_faces_inner(self, inner_indices):
        """生成船体内壁面片（反向法线）"""
        print("生成内壁面片...")

        for i in range(1, len(inner_indices) - 2):
            curr = inner_indices[i]
            next_seg = inner_indices[i + 1]

            # 检查是否有效
            if None in curr or None in next_seg:
                continue

            # 左侧内壁（反向）
            for j in range(4):
                v1 = curr[j]
                v2 = curr[j + 1]
                v3 = next_seg[j + 1]
                v4 = next_seg[j]
                self.add_face(v1, v4, v3, v2)  # 反向

            # 右侧内壁（反向）
            indices = [0, 5, 6, 7, 8]
            for j in range(4):
                v1 = curr[indices[j]]
                v2 = curr[indices[j + 1]]
                v3 = next_seg[indices[j + 1]]
                v4 = next_seg[indices[j]]
                self.add_face(v1, v4, v3, v2)  # 反向

    def generate_gunwale(self, outer_indices, inner_indices):
        """生成船舷边缘（连接内外壁）"""
        print("生成船舷边缘...")

        for i in range(1, len(outer_indices) - 1):
            outer = outer_indices[i]
            inner = inner_indices[i]

            if None in inner:
                continue

            # 左船舷边缘（索引4）
            if i < len(outer_indices) - 1:
                next_outer = outer_indices[i + 1]
                next_inner = inner_indices[i + 1]

                if None not in next_inner:
                    # 外壁左船舷 -> 内壁左船舷
                    self.add_face(outer[4], next_outer[4], next_inner[4], inner[4])

                    # 外壁右船舷 -> 内壁右船舷
                    self.add_face(outer[8], next_outer[8], next_inner[8], inner[8])

    def generate_dragon_head(self, first_segment):
        """生成龙头"""
        print("生成龙头...")

        # 龙头中心
        center = self.add_vertex(0.0, 0.0, -11.5)

        # 上嘴
        upper_tip = self.add_vertex(0.0, 0.6, -12.8)
        upper_left = self.add_vertex(-0.35, 0.5, -12.5)
        upper_right = self.add_vertex(0.35, 0.5, -12.5)

        # 下嘴
        lower_tip = self.add_vertex(0.0, -0.4, -12.8)
        lower_left = self.add_vertex(-0.35, -0.3, -12.5)
        lower_right = self.add_vertex(0.35, -0.3, -12.5)

        # 龙角
        horn_left_1 = self.add_vertex(-0.6, 0.9, -11.2)
        horn_left_2 = self.add_vertex(-0.7, 1.5, -10.8)
        horn_right_1 = self.add_vertex(0.6, 0.9, -11.2)
        horn_right_2 = self.add_vertex(0.7, 1.5, -10.8)

        # 龙头面片
        self.add_face(center, upper_left, upper_tip)
        self.add_face(center, upper_tip, upper_right)
        self.add_face(center, lower_left, lower_tip)
        self.add_face(center, lower_tip, lower_right)

        # 连接船头
        self.add_face(upper_left, first_segment[4], center)
        self.add_face(upper_right, first_segment[8], center)
        self.add_face(lower_left, first_segment[4], center)
        self.add_face(lower_right, first_segment[8], center)

        # 龙角
        self.add_face(upper_left, horn_left_1, horn_left_2)
        self.add_face(upper_right, horn_right_1, horn_right_2)

        return center

    def generate_dragon_tail(self, last_segment):
        """生成龙尾"""
        print("生成龙尾...")

        # 尾基座
        base_center = self.add_vertex(0.0, 0.0, 10.5)
        base_left = self.add_vertex(-0.5, -0.1, 10.5)
        base_right = self.add_vertex(0.5, -0.1, 10.5)

        # 第一层
        layer1_center = self.add_vertex(0.0, 0.4, 11.0)
        layer1_left = self.add_vertex(-0.6, 0.3, 11.0)
        layer1_right = self.add_vertex(0.6, 0.3, 11.0)

        # 第二层
        layer2_center = self.add_vertex(0.0, 0.8, 11.5)
        layer2_left = self.add_vertex(-0.7, 0.6, 11.5)
        layer2_right = self.add_vertex(0.7, 0.6, 11.5)

        # 顶端
        tip = self.add_vertex(0.0, 1.2, 12.0)

        # 连接船尾
        self.add_face(last_segment[0], base_center, base_left)
        self.add_face(last_segment[0], base_center, base_right)
        self.add_face(last_segment[4], base_left, layer1_left)
        self.add_face(last_segment[8], base_right, layer1_right)

        # 尾翼层次
        self.add_face(base_center, layer1_center, layer1_left, base_left)
        self.add_face(base_center, layer1_center, layer1_right, base_right)

        self.add_face(layer1_center, layer2_center, layer2_left, layer1_left)
        self.add_face(layer1_center, layer2_center, layer2_right, layer1_right)

        self.add_face(layer2_center, tip, layer2_left)
        self.add_face(layer2_center, tip, layer2_right)

        return tip

    def generate(self):
        """生成完整模型"""
        print("\n开始生成双层龙舟模型...")
        print("=" * 50)

        # 1. 生成外壁
        outer_indices = self.generate_hull_outer()

        # 2. 生成内壁
        inner_indices = self.generate_hull_inner(outer_indices)

        # 3. 生成外壁面片
        self.generate_hull_faces_outer(outer_indices)

        # 4. 生成内壁面片
        self.generate_hull_faces_inner(inner_indices)

        # 5. 生成船舷边缘
        self.generate_gunwale(outer_indices, inner_indices)

        # 6. 生成龙头
        self.generate_dragon_head(outer_indices[0])

        # 7. 生成龙尾
        self.generate_dragon_tail(outer_indices[-1])

        print("=" * 50)
        print(f"✓ 生成完成！")
        print(f"  顶点数: {self.vertex_count}")
        print(f"  面数: {len(self.faces)}")

    def save(self, filename):
        """保存为 OBJ 文件"""
        with open(filename, 'w', encoding='utf-8') as f:
            f.write("# 精美双层龙舟 3D 模型 - Fixed Version\n")
            f.write("# Complete Dragon Boat with Correct Topology\n\n")

            # 写入顶点
            f.write("# ========== 顶点 ==========\n")
            for v in self.vertices:
                f.write(v + "\n")

            f.write("\n")

            # 写入面片
            f.write("# ========== 面片 ==========\n")
            for face in self.faces:
                f.write(face + "\n")

        print(f"\n✓ 模型已保存到: {filename}")


if __name__ == "__main__":
    generator = DragonBoatGenerator()
    generator.generate()
    generator.save("dragon_boat_v4.obj")
