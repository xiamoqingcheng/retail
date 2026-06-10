import http from "@/api";

/**
 * @name 商品管理模块
 */
export namespace Goods {
  /** 商品实体 */
  export interface GoodsItem {
    id?: number | null;
    name: string;
    price: number;
    stock: number;
    safeStock?: number;
    categoryId?: number | null;
    shelfId: string;
    imageUrl: string;
    createTime?: string;
    updateTime?: string;
  }

  /** 分类实体 */
  export interface CategoryItem {
    id: number;
    name: string;
    sortOrder: number;
  }

  /** 分页查询请求参数 */
  export interface ReqGoodsParams {
    page: number;
    size: number;
    name?: string;
    categoryId?: number;
    sortField?: string;
    sortOrder?: string;
  }

  /** 分页查询响应体（MyBatis-Plus Page 结构） */
  export interface ResGoodsPage {
    records: GoodsItem[];
    total: number;
  }
}

/**
 * 分页查询商品
 * GET /api/goods/page
 */
export const getGoodsPage = (params: Goods.ReqGoodsParams) => {
  return http.get<Goods.ResGoodsPage>("/api/goods/page", params, { loading: false });
};

/**
 * 新增商品
 * POST /api/goods
 */
export const addGoods = (data: Partial<Goods.GoodsItem>) => {
  return http.post("/api/goods", data);
};

/**
 * 修改商品
 * PUT /api/goods
 */
export const updateGoods = (data: Partial<Goods.GoodsItem> & { id: number }) => {
  return http.put("/api/goods", data);
};

/**
 * 删除商品
 * DELETE /api/goods/{id}
 */
export const deleteGoods = (id: number | string) => {
  return http.delete(`/api/goods/${id}`);
};

/**
 * 上传商品图片
 * POST /api/upload  (FormData: file)
 * 返回 data 为图片 URL 字符串
 */
export const uploadGoodsImage = (params: FormData) => {
  return http.post<string>("/api/upload", params, { cancel: false });
};

/** 查询所有分类 */
export const getCategories = () => {
  return http.get<Goods.CategoryItem[]>("/api/goods/categories", {}, { loading: false });
};
