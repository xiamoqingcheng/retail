package com.retail.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.retail.server.dto.AdminWarningDTO;
import com.retail.server.entity.Warning;

import java.util.List;

/**
 * 库存告警业务层。
 */
public interface WarningService extends IService<Warning> {

	/**
	 * 查询所有未处理告警（管理端）。
	 */
	List<AdminWarningDTO> listPendingWarnings();
}
