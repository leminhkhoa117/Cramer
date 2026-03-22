package com.cramer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "speaking.session")
public class SpeakingSessionProperties {

    private int luaCost = 15;
    private boolean luaCheckOnCreate = true;
    private boolean luaChargeOnComplete = true;

    private PartPlan part1 = new PartPlan(30, 8, 12, false);
    private PartPlan part2 = new PartPlan(1, 1, 1, false);
    private PartPlan part3 = new PartPlan(15, 3, 6, true);

    public int getLuaCost() {
        return luaCost;
    }

    public void setLuaCost(int luaCost) {
        this.luaCost = luaCost;
    }

    public boolean isLuaCheckOnCreate() {
        return luaCheckOnCreate;
    }

    public void setLuaCheckOnCreate(boolean luaCheckOnCreate) {
        this.luaCheckOnCreate = luaCheckOnCreate;
    }

    public boolean isLuaChargeOnComplete() {
        return luaChargeOnComplete;
    }

    public void setLuaChargeOnComplete(boolean luaChargeOnComplete) {
        this.luaChargeOnComplete = luaChargeOnComplete;
    }

    public PartPlan getPart1() {
        return part1;
    }

    public void setPart1(PartPlan part1) {
        this.part1 = part1;
    }

    public PartPlan getPart2() {
        return part2;
    }

    public void setPart2(PartPlan part2) {
        this.part2 = part2;
    }

    public PartPlan getPart3() {
        return part3;
    }

    public void setPart3(PartPlan part3) {
        this.part3 = part3;
    }

    public static class PartPlan {

        private int bankSize;
        private int minSelected;
        private int maxSelected;
        private boolean deferUntilContext;

        public PartPlan() {
        }

        public PartPlan(int bankSize, int minSelected, int maxSelected, boolean deferUntilContext) {
            this.bankSize = bankSize;
            this.minSelected = minSelected;
            this.maxSelected = maxSelected;
            this.deferUntilContext = deferUntilContext;
        }

        public int getBankSize() {
            return bankSize;
        }

        public void setBankSize(int bankSize) {
            this.bankSize = bankSize;
        }

        public int getMinSelected() {
            return minSelected;
        }

        public void setMinSelected(int minSelected) {
            this.minSelected = minSelected;
        }

        public int getMaxSelected() {
            return maxSelected;
        }

        public void setMaxSelected(int maxSelected) {
            this.maxSelected = maxSelected;
        }

        public boolean isDeferUntilContext() {
            return deferUntilContext;
        }

        public void setDeferUntilContext(boolean deferUntilContext) {
            this.deferUntilContext = deferUntilContext;
        }
    }
}
