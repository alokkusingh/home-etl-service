package com.alok.home.grpc;

import lombok.extern.slf4j.Slf4j;
import com.alok.home.grpc.stub.ExpenseCategorizerGrpc;
import com.alok.home.grpc.stub.ExpenseCategorizerOuterClass;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExpenseCategorizerClient {

    @GrpcClient("exp-cat-grpc-server")
    private ExpenseCategorizerGrpc.ExpenseCategorizerBlockingStub expenseCategorizerStub;

    public String getExpenseCategory(String head) {

        final ExpenseCategorizerOuterClass.ExpenseCategorizationResponse response = expenseCategorizerStub.getExpenseCategoryUnary(ExpenseCategorizerOuterClass.ExpenseCategorizationRequest.newBuilder().setHead(head).build());

        return response.getCategory();
    }
}
