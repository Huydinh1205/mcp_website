package com.marketplace.negotiation;

public sealed interface TurnResult permits TurnResult.Ok, TurnResult.Err {
  record Ok(TurnOutcome value) implements TurnResult {}

  record Err(TurnErrorCode error) implements TurnResult {}

  static TurnResult ok(TurnOutcome value) {
    return new Ok(value);
  }

  static TurnResult err(TurnErrorCode error) {
    return new Err(error);
  }
}
