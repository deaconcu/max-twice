package com.prosper.learn.api.web;

import com.prosper.learn.api.client.NodeClient;
import com.prosper.learn.dto.NodeDTO;
import com.prosper.learn.dto.Response;
import com.prosper.learn.domain.util.Converter;
import com.prosper.learn.persistence.dataobject.NodeDO;
import com.prosper.learn.persistence.mapper.NodeMapper;
import com.prosper.learn.persistence.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedList;
import java.util.List;

@RestController
//@SaCheckLogin
@Slf4j
@RequiredArgsConstructor
public class NodeController implements NodeClient {

    private final NodeMapper nodeMapper;
    private final PostMapper postMapper;

    @Override
    public Response post(NodeDTO nodeDTO) {
        NodeDO node = Converter.INSTANCE.toNodeDO(nodeDTO);
        nodeMapper.insert(node);
        return Response.success;
    }

    public NodeDTO get(int id) {
        return Converter.INSTANCE.toNodeDTO(nodeMapper.getById(id));
    }

    public List<NodeDTO> getNodes(String ids) {
        List<Integer> idList = new LinkedList<>();
        String[] idArray = ids.split(",");
        for(String s: idArray) {
            try {
                idList.add(Integer.parseInt(s));
            } catch (Exception e) {
            }
        }
        List<NodeDO> nodes = nodeMapper.getByIds(idList);
        return Converter.INSTANCE.toNodeDTO(nodes);
    }
}
