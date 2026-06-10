package com.retail.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.retail.server.entity.Camera;
import com.retail.server.mapper.CameraMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CameraService {

    private final CameraMapper cameraMapper;

    public CameraService(CameraMapper cameraMapper) {
        this.cameraMapper = cameraMapper;
    }

    public List<Camera> listAll() {
        return cameraMapper.selectList(new LambdaQueryWrapper<Camera>().orderByAsc(Camera::getId));
    }

    public Camera getById(Long id) {
        return cameraMapper.selectById(id);
    }

    public Camera getByCameraNo(String cameraNo) {
        return cameraMapper.selectOne(new LambdaQueryWrapper<Camera>()
                .eq(Camera::getCameraNo, cameraNo).last("LIMIT 1"));
    }

    public Camera create(Camera camera) {
        cameraMapper.insert(camera);
        return camera;
    }

    public int update(Camera camera) {
        return cameraMapper.updateById(camera);
    }

    public int delete(Long id) {
        return cameraMapper.deleteById(id);
    }
}
