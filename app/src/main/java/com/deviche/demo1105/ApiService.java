package com.deviche.demo1105;


import io.reactivex.Observable;
import retrofit2.http.GET;

public interface ApiService {

    @GET("JustinRoom/JSCKit/master/capture/output.json")
    Observable<String> getVersionInfo();

}