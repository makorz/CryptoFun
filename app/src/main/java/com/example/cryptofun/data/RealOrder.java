package com.example.cryptofun.data;

public class RealOrder {

     String clientOrderId;
     float cumQty;
     float cumQuote;
     float executedQty;
     long orderId;
     float avgPrice;
     float price;
     boolean reduceOnly;
     String side;
     String positionSide;
     String status;
     float stopPrice;
     boolean closePosition;
     String symbol;
     String timeInForce;
     String type;
     String origType;
     float activatePrice;
     float priceRate;
     long updateTime;
     String workingType;
     boolean priceProtect;

//     "clientOrderId": "testOrder",
//             "cumQty": "0",
//             "cumQuote": "0",
//             "executedQty": "0",
//             "orderId": 22542179,
//             "avgPrice": "0.00000",
//             "origQty": "10",
//             "price": "0",
//             "reduceOnly": false,
//             "side": "BUY",
//             "positionSide": "SHORT",
//             "status": "NEW",
//             "stopPrice": "9300",        // please ignore when order type is TRAILING_STOP_MARKET
//             "closePosition": false,   // if Close-All
//             "symbol": "BTCUSDT",
//             "timeInForce": "GTC",
//             "type": "TRAILING_STOP_MARKET",
//             "origType": "TRAILING_STOP_MARKET",
//             "activatePrice": "9020",    // activation price, only return with TRAILING_STOP_MARKET order
//             "priceRate": "0.3",         // callback rate, only return with TRAILING_STOP_MARKET order
//             "updateTime": 1566818724722,
//             "workingType": "CONTRACT_PRICE",
//             "priceProtect": false            // if conditional order trigger is protected

     public RealOrder(String clientOrderId, float cumQty, float cumQuote, float executedQty, long orderId, float avgPrice, float price, boolean reduceOnly, String side, String positionSide, String status, float stopPrice, boolean closePosition, String symbol, String timeInForce, String type, String origType, float activatePrice, float priceRate, long updateTime, String workingType, boolean priceProtect) {
          this.clientOrderId = clientOrderId;
          this.cumQty = cumQty;
          this.cumQuote = cumQuote;
          this.executedQty = executedQty;
          this.orderId = orderId;
          this.avgPrice = avgPrice;
          this.price = price;
          this.reduceOnly = reduceOnly;
          this.side = side;
          this.positionSide = positionSide;
          this.status = status;
          this.stopPrice = stopPrice;
          this.closePosition = closePosition;
          this.symbol = symbol;
          this.timeInForce = timeInForce;
          this.type = type;
          this.origType = origType;
          this.activatePrice = activatePrice;
          this.priceRate = priceRate;
          this.updateTime = updateTime;
          this.workingType = workingType;
          this.priceProtect = priceProtect;
     }

     public String getClientOrderId() {
          return clientOrderId;
     }

     public void setClientOrderId(String clientOrderId) {
          this.clientOrderId = clientOrderId;
     }

     public float getCumQty() {
          return cumQty;
     }

     public void setCumQty(float cumQty) {
          this.cumQty = cumQty;
     }

     public float getCumQuote() {
          return cumQuote;
     }

     public void setCumQuote(float cumQuote) {
          this.cumQuote = cumQuote;
     }

     public float getExecutedQty() {
          return executedQty;
     }

     public void setExecutedQty(float executedQty) {
          this.executedQty = executedQty;
     }

     public long getOrderId() {
          return orderId;
     }

     public void setOrderId(long orderId) {
          this.orderId = orderId;
     }

     public float getAvgPrice() {
          return avgPrice;
     }

     public void setAvgPrice(float avgPrice) {
          this.avgPrice = avgPrice;
     }

     public float getPrice() {
          return price;
     }

     public void setPrice(float price) {
          this.price = price;
     }

     public boolean isReduceOnly() {
          return reduceOnly;
     }

     public void setReduceOnly(boolean reduceOnly) {
          this.reduceOnly = reduceOnly;
     }

     public String getSide() {
          return side;
     }

     public void setSide(String side) {
          this.side = side;
     }

     public String getPositionSide() {
          return positionSide;
     }

     public void setPositionSide(String positionSide) {
          this.positionSide = positionSide;
     }

     public String getStatus() {
          return status;
     }

     public void setStatus(String status) {
          this.status = status;
     }

     public float getStopPrice() {
          return stopPrice;
     }

     public void setStopPrice(float stopPrice) {
          this.stopPrice = stopPrice;
     }

     public boolean isClosePosition() {
          return closePosition;
     }

     public void setClosePosition(boolean closePosition) {
          this.closePosition = closePosition;
     }

     public String getSymbol() {
          return symbol;
     }

     public void setSymbol(String symbol) {
          this.symbol = symbol;
     }

     public String getTimeInForce() {
          return timeInForce;
     }

     public void setTimeInForce(String timeInForce) {
          this.timeInForce = timeInForce;
     }

     public String getType() {
          return type;
     }

     public void setType(String type) {
          this.type = type;
     }

     public String getOrigType() {
          return origType;
     }

     public void setOrigType(String origType) {
          this.origType = origType;
     }

     public float getActivatePrice() {
          return activatePrice;
     }

     public void setActivatePrice(float activatePrice) {
          this.activatePrice = activatePrice;
     }

     public float getPriceRate() {
          return priceRate;
     }

     public void setPriceRate(float priceRate) {
          this.priceRate = priceRate;
     }

     public long getUpdateTime() {
          return updateTime;
     }

     public void setUpdateTime(long updateTime) {
          this.updateTime = updateTime;
     }

     public String getWorkingType() {
          return workingType;
     }

     public void setWorkingType(String workingType) {
          this.workingType = workingType;
     }

     public boolean isPriceProtect() {
          return priceProtect;
     }

     public void setPriceProtect(boolean priceProtect) {
          this.priceProtect = priceProtect;
     }
}
