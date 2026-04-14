from typing import Optional
from langchain_mistralai import ChatMistralAI
from pydantic import BaseModel, Field  # ✅ directly pydantic use karo

class Expense(BaseModel):
    amount: Optional[str] = Field(title="expense", description="Expense made on the transaction")
    merchant: Optional[str] = Field(title="merchant", description="Merchant name")
    currency: Optional[str] = Field(title="currency", description="Currency of the transaction")

    def serialize(self):
        return {
            "amount": self.amount,
            "merchant": self.merchant,
            "currency": self.currency
        }