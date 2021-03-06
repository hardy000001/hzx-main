/**
 *
 */
package com.lq.lss.controller.stock;

import com.lq.easyui.dto.ResultDto;
import com.lq.lss.constant.PermResourceConst;
import com.lq.lss.controller.util.AjaxModelAndViewUtils;
import com.lq.lss.controller.util.LoginSessionUtils;
import com.lq.lss.core.service.AdminAuditLogService;
import com.lq.lss.core.service.StockTransferDetailService;
import com.lq.lss.core.service.StockTransferService;
import com.lq.lss.enums.TradeType;
import com.lq.lss.model.CStockTransfer;
import com.lq.lss.model.CStockTransferDetail;
import com.lq.lss.model.SessionUser;
import com.lq.lss.model.ShiroUser;
import com.lq.lss.controller.shiro.ShiroBaseController;
import com.lq.springmvc.i18n.I18nModelAndView;
import com.lq.util.JSONUtil;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ch
 */
@Controller
@RequestMapping(value = "/user/stock/transferin.htm")
public class TransfeInrController extends ShiroBaseController<CStockTransfer, String, StockTransferService> {


    @Resource
    private StockTransferService stockTransferService;

    @Resource
    private StockTransferDetailService stockTransferDetailService;

    @Resource
    private AdminAuditLogService adminAuditLogService;

    @Value("/stock/transfer_in")
    private String indexView;

    @RequestMapping
    public ModelAndView index(HttpServletRequest request, HttpServletResponse response) {

        ShiroUser shiroUser = getCurrentUser();

        Map<String, Object> modelMap = new HashMap<String, Object>();

        modelMap.put("add", PermResourceConst.CENTER_TRANS_IN_ADD);
        modelMap.put("update", PermResourceConst.CENTER_TRANS_IN_UPDATE);
        modelMap.put("del", PermResourceConst.CENTER_TRANS_IN_DEL);
        modelMap.put("check", PermResourceConst.CENTER_TRANS_IN_CHECK);

        modelMap.put("deptid", shiroUser.getSessionUser().getDeptId());
        modelMap.put("centerId", shiroUser.getSessionUser().getCenterId());

        if (useI18n) {
            return new I18nModelAndView(request, indexView, modelMap);
        }
        return new ModelAndView(indexView, modelMap);
    }


    /**
     * 删除单据
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(params = "method=remove")
    @RequiresPermissions(PermResourceConst.CENTER_TRANS_IN_DEL)
    public ModelAndView remove(HttpServletRequest request, HttpServletResponse response) {

        ResultDto<String> result = new ResultDto<String>();


        String idStr = ServletRequestUtils.getStringParameter(request, "id", null);

        try {
            if (!StringUtils.hasLength(idStr)) {
                result = new ResultDto<String>(false, "id或ids不能为空");
                return AjaxModelAndViewUtils.writeMsgReturnNull(response, JSONUtil.toJSonString(result));
            }

            stockTransferService.deleteStockTransferByIdsRdTx(idStr);
            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
            e.printStackTrace();
        }

        return AjaxModelAndViewUtils.writeMsgReturnNull(response, JSONUtil.toJSonString(result));
    }

    /**
     * 查询调拨单明细
     *
     * @param request  请求
     * @param response 输出
     * @return 返回
     */
    @RequestMapping(params = "method=queryTransferDetail")
    public ModelAndView queryTransferDetail(HttpServletRequest request, HttpServletResponse response) {

        SessionUser sessionUser = LoginSessionUtils.getUserInSession(request);

        String tsfSerialNo = ServletRequestUtils.getStringParameter(request, "tsfSerialNo", null);

        List<CStockTransferDetail> cStockTransferDetailList = new ArrayList<CStockTransferDetail>();
        try {

            cStockTransferDetailList = stockTransferDetailService.queryDetailList(tsfSerialNo);

            adminAuditLogService.log(sessionUser.getUserId(), TradeType.STOCK_TRANSFER_IN.getType(), "删除了中心内部调入单", tsfSerialNo, 0L);

        } catch (Exception e) {
            logger.error("查询调拨明细出现错误，错误信息:" + e.getMessage());
            e.printStackTrace();
        }

        return AjaxModelAndViewUtils.writeMsgReturnNull(response, JSONUtil.toJSonString(cStockTransferDetailList));
    }

    /**
     * 更新调拨单据状态
     *
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(params = "method=audit")
    @RequiresPermissions(PermResourceConst.CENTER_TRANS_IN_CHECK)
    public ModelAndView audit(HttpServletRequest request, HttpServletResponse response) {

        ResultDto<String> result = new ResultDto<String>();

        SessionUser sessionUser = LoginSessionUtils.getUserInSession(request);


        String tsfSerialno = ServletRequestUtils.getStringParameter(request, "id", null);
        String status = ServletRequestUtils.getStringParameter(request, "status", null);
        try {
            if (!StringUtils.hasLength(tsfSerialno)) {
                result = new ResultDto<String>(false, "单价流水号不能为空");
                return AjaxModelAndViewUtils.writeMsgReturnNull(response, JSONUtil.toJSonString(result));
            }
            CStockTransfer cStockTransfer = new CStockTransfer();
            cStockTransfer.setTsfSerialno(tsfSerialno);
            cStockTransfer.setStatus(status);
            cStockTransfer.setAuditingoper(sessionUser.getUserId()+"");
            cStockTransfer.setFromDeptid(sessionUser.getCenterId());

           
            stockTransferService.auditStockTransferInByIdRdTx(cStockTransfer);

            adminAuditLogService.log(sessionUser.getUserId(), TradeType.STOCK_TRANSFER_IN.getType(), "审核了中心内部调入单", cStockTransfer, 0L);

            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMsg(e.getMessage());
            e.printStackTrace();
        }

        return AjaxModelAndViewUtils.writeMsgReturnNull(response, JSONUtil.toJSonString(result));
    }

    
    
    
    @RequestMapping(params = "method=antiAuditTraIn")
   	@RequiresPermissions(PermResourceConst.CENTER_TRANS_IN_CHECK)
   	public ModelAndView antiAuditTraIn(HttpServletRequest request,HttpServletResponse response) {
   		
   		//获取流水号
   		String tsfSerialno = ServletRequestUtils.getStringParameter(request,
   				"tsfSerialno", "");
   		String status = ServletRequestUtils.getStringParameter(request,
   				"status", "");
   		SessionUser user = LoginSessionUtils.getUserInSession(request);
   		ResultDto<String> result = null;
   		
   		try {
   			CStockTransfer cStockTransfer=new CStockTransfer();
   			
   			cStockTransfer.setTsfSerialno(tsfSerialno);
   			cStockTransfer.setStatus("1");
   			cStockTransfer.setAuditingoper(null);
   			cStockTransfer.setAuditingtime(null);
   			result=stockTransferService.antiAuditStockTransferInByIdRdTx(cStockTransfer);
   			

   		} catch (Exception e) {
   			e.printStackTrace();
   			logger.error("反审核中心内部调出数据出现异常"+e.getMessage());
   			result=new ResultDto<String>(false,"审核出现异常");

   		}

   		return AjaxModelAndViewUtils.writeMsgReturnNull(response,
   				JSONUtil.toJSonString(result));
   	}
}
