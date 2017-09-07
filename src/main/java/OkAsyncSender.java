package io.airbrake.javabrake;

import java.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

class OkAsyncSender extends OkSender implements AsyncSender {
  public OkAsyncSender(int projectId, String projectKey) {
    super(projectId, projectKey);
  }

  @Override
  public CompletableFuture<Notice> send(Notice notice) {
    CompletableFuture<Notice> future = new CompletableFuture<>();

    if (notice == null) {
      future.completeExceptionally(new IOException("notice is null"));
      return future;
    }

    long utime = System.currentTimeMillis() / 1000L;
    if (utime < this.rateLimitReset.get()) {
      notice.exception = OkSender.ipRateLimitedException;
      future.completeExceptionally(notice.exception);
      return future;
    }

    OkAsyncSender sender = this;
    OkAsyncSender.okhttp
        .newCall(this.buildRequest(notice))
        .enqueue(
            new Callback() {
              @Override
              public void onFailure(Call call, IOException e) {
                notice.exception = e;
                future.completeExceptionally(notice.exception);
              }

              @Override
              public void onResponse(Call call, Response resp) {
                sender.parseResponse(resp, notice);
                if (notice.exception != null) {
                  future.completeExceptionally(notice.exception);
                } else {
                  future.complete(notice);
                }
              }
            });
    return future;
  }
}
