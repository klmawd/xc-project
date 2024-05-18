package com.xuecheng.ucenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xuecheng.ucenter.model.po.XcMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author itcast
 */
public interface XcMenuMapper extends BaseMapper<XcMenu> {

    //根据用户id查询权限
    @Select("SELECT	code FROM xc_menu WHERE id IN (SELECT menu_id FROM xc_permission WHERE role_id IN ( SELECT role_id FROM xc_user_role WHERE user_id = #{userId} ))")
    List<String> selectPermissionByUserId(@Param("userId") String userId);

}
