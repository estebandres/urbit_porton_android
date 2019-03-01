package com.urbit_iot.porton.umods.domain.usecase;

import androidx.annotation.NonNull;

import com.urbit_iot.porton.RxUseCase;
import com.urbit_iot.porton.SimpleUseCase;
import com.urbit_iot.porton.appuser.data.source.AppUserRepository;
import com.urbit_iot.porton.data.rpc.GetGateStatusRPC;
import com.urbit_iot.porton.data.source.UModsRepository;
import com.urbit_iot.porton.util.schedulers.BaseSchedulerProvider;

import javax.inject.Inject;

import rx.Observable;

import static com.google.common.base.Preconditions.checkNotNull;

public class GateStatusUpdates extends SimpleUseCase<GateStatusUpdates.RequestValues, GateStatusUpdates.ResponseValues> {
    private final UModsRepository mUModsRepository;
    private final BaseSchedulerProvider schedulerProvider;

    @Inject
    public GateStatusUpdates(@NonNull UModsRepository uModsRepository,
                         @NonNull AppUserRepository appUserRepository,
                         @NonNull BaseSchedulerProvider schedulerProvider) {
        super(schedulerProvider.io(), schedulerProvider.ui());
        this.mUModsRepository = checkNotNull(uModsRepository, "uModsRepository cannot be null!");
        this.schedulerProvider = schedulerProvider;
    }

    @Override
    protected Observable<ResponseValues> buildUseCase(RequestValues requestValues) {
        return mUModsRepository.getUModGateStatusUpdates()
                .map(response -> new ResponseValues(response.getRequestTag(), response.getResponseResult()));
    }

    public final static class RequestValues implements RxUseCase.RequestValues{
    }

    public final static class ResponseValues implements RxUseCase.ResponseValues{
        private String umodUUID;
        private GetGateStatusRPC.Result gateStatus;

        public ResponseValues(String umodUUID, GetGateStatusRPC.Result gateStatus) {
            this.umodUUID = umodUUID;
            this.gateStatus = gateStatus;
        }

        public String getUmodUUID() {
            return umodUUID;
        }

        public GetGateStatusRPC.Result getGateStatus() {
            return gateStatus;
        }

    }
}
