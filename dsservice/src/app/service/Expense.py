from typing import Optional, Literal
from langchain_mistralai import ChatMistralAI
from pydantic import BaseModel, Field  # ✅ directly pydantic use karo

class Expense(BaseModel):
    amount: Optional[str] = Field(title="expense", description="Expense made on the transaction")
    merchant: Optional[str] = Field(title="merchant", description="Merchant name")
    currency: Optional[str] = Field(title="currency", description="Currency of the transaction")
    transaction_type: Optional[Literal["credit", "debit"]] = Field(
        default=None,
        title="transaction_type",
        description="Whether money was credited (received) or debited (spent) in the transaction"
    )

    def serialize(self):
        return {
            "amount": self.amount,
            "merchant": self.merchant,
            "currency": self.currency,
            "transaction_type": self.transaction_type
        }