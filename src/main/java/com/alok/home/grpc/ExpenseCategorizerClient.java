package com.alok.home.grpc;

import com.alok.home.grpc.stub.ExpenseCategorizerGrpc;
import com.alok.home.grpc.stub.ExpenseCategorizerOuterClass.ExpenseCategorizationRequest;
import com.alok.home.grpc.stub.ExpenseCategorizerOuterClass.ExpenseCategorizationResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class ExpenseCategorizerClient {

    private static final Logger log = LoggerFactory.getLogger(ExpenseCategorizerClient.class);

    @GrpcClient("exp-cat-grpc-server")
    private ExpenseCategorizerGrpc.ExpenseCategorizerBlockingStub expenseCategorizerStub;

    @GrpcClient("exp-cat-grpc-server")
    private ExpenseCategorizerGrpc.ExpenseCategorizerStub asyncExpenseCategorizerStub;

    public record PredictionResult(String category, float confidence, boolean needsReview) {}

    // 1. Backwards-compatible overload for any existing callers passing only 'head'
    public String getExpenseCategory(String head) {
        return getExpenseCategory(head, "", 0.0);
    }

    // 2. 3-parameter overload returning category string directly
    public String getExpenseCategory(String head, String comment, Double amount) {
        return predictExpenseCategory(head, comment, amount).category();
    }

    // 3. Full 3-parameter prediction with confidence & needsReview flag
    public PredictionResult predictExpenseCategory(String head, String comment, Double amount) {
        ExpenseCategorizationRequest request = ExpenseCategorizationRequest.newBuilder()
                .setHead(head != null ? head : "")
                .setComment(comment != null ? comment : "")
                .setAmount(amount != null ? amount : 0.0)
                .build();

        ExpenseCategorizationResponse response = expenseCategorizerStub.getExpenseCategoryUnary(request);

        if (response.getNeedsReview()) {
            log.warn("Low confidence ({}) categorizing expense [head='{}', amount={}] -> '{}'",
                    String.format("%.2f", response.getConfidence()), head, amount, response.getCategory());
        }

        return new PredictionResult(
                response.getCategory(),
                response.getConfidence(),
                response.getNeedsReview()
        );
    }

    public List<PredictionResult> predictExpenseCategoriesBatch(List<ExpenseCategorizationRequest> requests) {
        if (requests.isEmpty()) {
            return Collections.emptyList();
        }

        List<PredictionResult> results = Collections.synchronizedList(new ArrayList<>(requests.size()));
        CompletableFuture<List<PredictionResult>> future = new CompletableFuture<>();

        StreamObserver<ExpenseCategorizationRequest> requestObserver = asyncExpenseCategorizerStub.getExpenseCategory(
                new StreamObserver<>() {
                    @Override
                    public void onNext(ExpenseCategorizationResponse response) {
                        results.add(new PredictionResult(
                                response.getCategory(),
                                response.getConfidence(),
                                response.getNeedsReview()
                        ));
                    }

                    @Override
                    public void onError(Throwable t) {
                        future.completeExceptionally(t);
                    }

                    @Override
                    public void onCompleted() {
                        future.complete(results);
                    }
                }
        );

        for (ExpenseCategorizationRequest req : requests) {
            requestObserver.onNext(req);
        }
        requestObserver.onCompleted();

        return future.join(); // Safe on Java 25 virtual threads (unmounts without blocking OS thread)
    }

}
