package com.hanyun.platform.pay.service.ybpay.weixin.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hanyun.platform.pay.adapt.yeepay.YeepayCommon;
import com.hanyun.platform.pay.adapt.yeepay.model.YeeCommonResCode;
import com.hanyun.platform.pay.adapt.yeepay.protocol.YeeResultNotifyRes;
import com.hanyun.platform.pay.consts.PaymentNewConsts;
import com.hanyun.platform.pay.dao.PayTransactionNewDao;
import com.hanyun.platform.pay.dao.PaymentNewDao;
import com.hanyun.platform.pay.domain.PayTransactionNew;
import com.hanyun.platform.pay.domain.PaymentNew;
import com.hanyun.platform.pay.logic.trade.TradeServiceAssistLogicYb;
import com.hanyun.platform.pay.service.ybpay.alipay.impl.YbAlipayCallbackServiceImpl;
import com.hanyun.platform.pay.service.ybpay.weixin.YbWeixinCallbackService;
import com.hanyun.platform.pay.vo.trade.YbTradePayAlipayExtData;

/**
 * 
 * @date 2018年8月8日
 * 
 * @apiDefine Common 微信支付回調處理接口
 * @author litao@hanyun.com
 */
@Service
public class YbWeixinCallbackServiceImpl implements YbWeixinCallbackService{
	private static final Logger LOGGER = LoggerFactory.getLogger(YbWeixinCallbackServiceImpl.class);
	@Autowired
	private PayTransactionNewDao payTransactionNewDao;
	
	@Autowired
	private PaymentNewDao paymentNewDao;
	
	@Autowired
	private TradeServiceAssistLogicYb tradeServiceAssistLogicYb;
	
	@Override
	public void dealPayResultCallback(YeeResultNotifyRes req) {
		
		PayTransactionNew trans = payTransactionNewDao.selectByTransId(req.getRequestNo());
        
		if (PaymentNewConsts.TRANS_STATUS_FINISH.equals(trans.getStatus()) ||
				req.getCode().equals(YeeCommonResCode.YEE_ORDER_PAY_SUCCESS)) {
            return ;
        }
		
        PaymentNew payment = paymentNewDao.selectByPayId(trans.getPayId());
        
		if(!req.getCode().equals(YeepayCommon.RES_CODE)){
			tradeServiceAssistLogicYb.updatePayTransWhenPayFail(trans);
			
			// 通知订单系统
	        YbTradePayAlipayExtData extdata = new YbTradePayAlipayExtData();
		    extdata.setYbAliOrderId(req.getRequestNo());
	        tradeServiceAssistLogicYb.notifyOrderWhenPayFinish(payment, trans, extdata);
		}else{
			 // 更新支付流水状态
	        tradeServiceAssistLogicYb.updatePayTransWhenPayFinish(trans);
	        // 更新支付单状态
	        tradeServiceAssistLogicYb.updatePaymentWhenPayFinish(payment, trans);
			LOGGER.info("============> notify order =====>"+trans.toString());
	        // 通知订单系统
	        YbTradePayAlipayExtData extdata = new YbTradePayAlipayExtData();
		    extdata.setYbAliOrderId(req.getRequestNo());
	        tradeServiceAssistLogicYb.notifyOrderWhenPayFinish(payment, trans, extdata);
		}
	}

}
