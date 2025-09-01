#!/bin/bash

# 批量创建 DTO 请求类脚本
# 此脚本用于验证所有请求 DTO 类文件是否已成功创建

echo "=== 验证所有请求 DTO 类文件 ==="
echo ""

# 定义基础目录
BASE_DIR="/Users/jia/workspace/max-twice/backend/learn-dto/src/main/java/com/prosper/learn/dto/request"

# 定义所有要检查的文件
FILES=(
    "FollowRequest.java"
    "SubscribeRequest.java"
    "UpdateSubscriptionRequest.java"
    "CreateRoadmapRequest.java"
    "UpdateRoadmapRequest.java"
    "PinRoadmapRequest.java"
    "UpvoteRequest.java"
    "ApproveRequest.java"
    "RecordViewRequest.java"
    "SyncDateRequest.java"
    "ChatRequest.java"
    "CreateCommentRequest.java"
    "ApproveCommentRequest.java"
    "ApprovePostRequest.java"
    "StartCourseRequest.java"
    "MarkNodeCompleteRequest.java"
    "CreateCourseApplicationRequest.java"
    "SendSystemMessageRequest.java"
    "ReplyCourseApplicationRequest.java"
    "ApproveProfessionRequest.java"
    "ApproveCourseRequest.java"
    "CreateSubcourseRequest.java"
)

# 检查文件是否存在
SUCCESS_COUNT=0
TOTAL_COUNT=${#FILES[@]}

for file in "${FILES[@]}"; do
    if [ -f "$BASE_DIR/$file" ]; then
        echo "✓ $file - 已创建"
        SUCCESS_COUNT=$((SUCCESS_COUNT + 1))
    else
        echo "✗ $file - 未找到"
    fi
done

echo ""
echo "=== 总结 ==="
echo "成功创建: $SUCCESS_COUNT/$TOTAL_COUNT 个文件"

if [ $SUCCESS_COUNT -eq $TOTAL_COUNT ]; then
    echo "🎉 所有请求 DTO 类文件创建成功！"
    exit 0
else
    echo "❌ 有部分文件创建失败，请检查"
    exit 1
fi