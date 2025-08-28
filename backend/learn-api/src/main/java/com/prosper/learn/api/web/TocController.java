package com.prosper.learn.api.web;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.prosper.learn.api.client.TocClient;
import com.prosper.learn.common.Utils;
import com.prosper.learn.common.exception.ErrorCode;
import com.prosper.learn.dto.Response;
import com.prosper.learn.persistence.dataobject.CourseDO;
import com.prosper.learn.persistence.dataobject.CourseTocDO;
import com.prosper.learn.persistence.dataobject.UserCourseTocDO;
import com.prosper.learn.persistence.mapper.CourseMapper;
import com.prosper.learn.persistence.mapper.CourseTocMapper;
import com.prosper.learn.persistence.mapper.UserCourseTocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class TocController implements TocClient {

    private final CourseMapper courseMapper;
    private final UserCourseTocMapper userCourseTocMapper;
    private final CourseTocMapper courseTocMapper;
    private final ObjectMapper objectMapper;

    @Override
    public Response<Object> post(Long courseId, String indexArray) {
        CourseDO courseDO = courseMapper.getById(courseId);
        if (courseDO == null) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        UserCourseTocDO userCourseTocDO = userCourseTocMapper.getByUserAndCourse(StpUtil.getLoginIdAsInt(), courseId);
        String toc = userCourseTocDO.getToc();
        String[] tocHashes = toc.split(",");

        String[] indexStrings = indexArray.split(",");
        if (indexStrings.length > 9) {
            throw ErrorCode.SYSTEM_ERROR.exception();
        }

        int[] indexes = new int[indexStrings.length];
        for (int i = 0; i < indexStrings.length; i++) {
            try {
                indexes[i] = Integer.parseInt(indexStrings[i]);
                if (Math.abs(indexes[i]) > tocHashes.length) {
                    throw ErrorCode.SYSTEM_ERROR.exception();
                }
            } catch (NumberFormatException e) {
                throw ErrorCode.SYSTEM_ERROR.exception();
            }
        }

        ObjectNode s = objectMapper.createObjectNode();
        s.set(Long.toString(courseDO.getRootNode()), objectMapper.createObjectNode());

        String tocStr = s.toString();
        String defaultTosHash = Utils.hashSHA(tocStr);

        if (courseTocMapper.get(defaultTosHash) == null)
            courseTocMapper.insert(new CourseTocDO(defaultTosHash, tocStr));

        String[] newTocArr = new String[indexes.length];
        int count = 0;
        for (int i = 0; i < indexes.length; i++) {
            if (indexes[i] != 0) {
                newTocArr[i] = tocHashes[Math.abs(indexes[i]) - 1];
            } else {
                newTocArr[i] = defaultTosHash;
                count ++;
            }
        }
        courseTocMapper.incrRef(defaultTosHash, count);

        String newToc = String.join(",", newTocArr);
        userCourseTocDO.setToc(newToc);
        userCourseTocMapper.update(userCourseTocDO);

        return Response.success;
    }
}
