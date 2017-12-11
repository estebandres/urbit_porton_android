package com.urbit_iot.onekey.data.rpc;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.urbit_iot.onekey.util.GlobalConstants;

/**
 * Created by andresteve07 on 12/10/17.
 */

public class SysGetInfoRPC extends RPC {
    public static class Arguments{
        public Arguments(){}
    }

    public static class Request extends RPC.Request{
        @SerializedName(GlobalConstants.RPC_REQ_ARGS_ATTR_NAME)
        private SysGetInfoRPC.Arguments methodArguments;

        public Request(Arguments methodArguments, String callTag, int callID) {
            super("Sys.GetInxxxfo",callTag,callID);
            this.methodArguments = methodArguments;
        }

        public Arguments getMethodArguments() {
            return methodArguments;
        }

        public void setMethodArguments(Arguments methodArguments) {
            this.methodArguments = methodArguments;
        }
    }

    public static class Result {
        @SerializedName("app")
        @Expose
        private String app;
        @SerializedName("fw_version")
        @Expose
        private String fwVersion;
        @SerializedName("fw_id")
        @Expose
        private String fwId;
        @SerializedName("mac")
        @Expose
        private String mac;
        @SerializedName("arch")
        @Expose
        private String arch;
        @SerializedName("uptime")
        @Expose
        private Integer uptime;
        @SerializedName("ram_size")
        @Expose
        private Integer ramSize;
        @SerializedName("ram_free")
        @Expose
        private Integer ramFree;
        @SerializedName("ram_min_free")
        @Expose
        private Integer ramMinFree;
        @SerializedName("fs_size")
        @Expose
        private Integer fsSize;
        @SerializedName("fs_free")
        @Expose
        private Integer fsFree;
        @SerializedName("wifi")
        @Expose
        private Wifi wifi;
        @SerializedName("eth")
        @Expose
        private Eth eth;

        public Result(String app, String fwVersion, String fwId, String mac, String arch,
                      Integer uptime, Integer ramSize, Integer ramFree, Integer ramMinFree,
                      Integer fsSize, Integer fsFree, Wifi wifi, Eth eth) {
            this.app = app;
            this.fwVersion = fwVersion;
            this.fwId = fwId;
            this.mac = mac;
            this.arch = arch;
            this.uptime = uptime;
            this.ramSize = ramSize;
            this.ramFree = ramFree;
            this.ramMinFree = ramMinFree;
            this.fsSize = fsSize;
            this.fsFree = fsFree;
            this.wifi = wifi;
            this.eth = eth;
        }

        public String getApp() {
            return app;
        }

        public void setApp(String app) {
            this.app = app;
        }

        public String getFwVersion() {
            return fwVersion;
        }

        public void setFwVersion(String fwVersion) {
            this.fwVersion = fwVersion;
        }

        public String getFwId() {
            return fwId;
        }

        public void setFwId(String fwId) {
            this.fwId = fwId;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getArch() {
            return arch;
        }

        public void setArch(String arch) {
            this.arch = arch;
        }

        public Integer getUptime() {
            return uptime;
        }

        public void setUptime(Integer uptime) {
            this.uptime = uptime;
        }

        public Integer getRamSize() {
            return ramSize;
        }

        public void setRamSize(Integer ramSize) {
            this.ramSize = ramSize;
        }

        public Integer getRamFree() {
            return ramFree;
        }

        public void setRamFree(Integer ramFree) {
            this.ramFree = ramFree;
        }

        public Integer getRamMinFree() {
            return ramMinFree;
        }

        public void setRamMinFree(Integer ramMinFree) {
            this.ramMinFree = ramMinFree;
        }

        public Integer getFsSize() {
            return fsSize;
        }

        public void setFsSize(Integer fsSize) {
            this.fsSize = fsSize;
        }

        public Integer getFsFree() {
            return fsFree;
        }

        public void setFsFree(Integer fsFree) {
            this.fsFree = fsFree;
        }

        public Wifi getWifi() {
            return wifi;
        }

        public void setWifi(Wifi wifi) {
            this.wifi = wifi;
        }

        public Eth getEth() {
            return eth;
        }

        public void setEth(Eth eth) {
            this.eth = eth;
        }

        @Override
        public String toString() {
            return "MosInfo{" +
                    "app='" + app + '\'' +
                    ", fwVersion='" + fwVersion + '\'' +
                    ", fwId='" + fwId + '\'' +
                    ", mac='" + mac + '\'' +
                    ", arch='" + arch + '\'' +
                    ", uptime=" + uptime +
                    ", ramSize=" + ramSize +
                    ", ramFree=" + ramFree +
                    ", ramMinFree=" + ramMinFree +
                    ", fsSize=" + fsSize +
                    ", fsFree=" + fsFree +
                    ", wifi=" + wifi +
                    ", eth=" + eth +
                    '}';
        }

        public class Eth {
            @SerializedName("ip")
            @Expose
            private String ip;

            public Eth(String ip) {
                this.ip = ip;
            }

            public String getIp() {
                return ip;
            }

            public void setIp(String ip) {
                this.ip = ip;
            }

            @Override
            public String toString() {
                return "Eth{" +
                        "ip='" + ip + '\'' +
                        '}';
            }
        }

        public class Wifi {
            @SerializedName("sta_ip")
            @Expose
            private String staIp;
            @SerializedName("ap_ip")
            @Expose
            private String apIp;
            @SerializedName("status")
            @Expose
            private String status;
            @SerializedName("ssid")
            @Expose
            private String ssid;

            public Wifi(String staIp, String apIp, String status, String ssid) {
                this.staIp = staIp;
                this.apIp = apIp;
                this.status = status;
                this.ssid = ssid;
            }

            public String getStaIp() {
                return staIp;
            }

            public void setStaIp(String staIp) {
                this.staIp = staIp;
            }

            public String getApIp() {
                return apIp;
            }

            public void setApIp(String apIp) {
                this.apIp = apIp;
            }

            public String getStatus() {
                return status;
            }

            public void setStatus(String status) {
                this.status = status;
            }

            public String getSsid() {
                return ssid;
            }

            public void setSsid(String ssid) {
                this.ssid = ssid;
            }

            @Override
            public String toString() {
                return "Wifi{" +
                        "staIp='" + staIp + '\'' +
                        ", apIp='" + apIp + '\'' +
                        ", status='" + status + '\'' +
                        ", ssid='" + ssid + '\'' +
                        '}';
            }
        }
    }

    public static class SuccessResponse extends RPC.SuccessResponse{
        @SerializedName(GlobalConstants.RPC_SUCC_RESP_RESULT_ATTR_NAME)
        private SysGetInfoRPC.Result responseResult;

        public SuccessResponse(Result responseResult, String callTag, ResponseError responseError) {
            super(callTag, responseError);
            this.responseResult = responseResult;
        }

        public Result getResponseResult() {
            return responseResult;
        }

        public void setResponseResult(Result responseResult) {
            this.responseResult = responseResult;
        }

        @Override
        public String toString() {
            return "SuccessResponse{" +
                    "responseResult=" + responseResult +
                    "} " + super.toString();
        }
    }
}