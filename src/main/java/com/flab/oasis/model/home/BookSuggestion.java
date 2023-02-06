package com.flab.oasis.model.home;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flab.oasis.constant.SuggestionType;
import com.flab.oasis.model.Book;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BookSuggestion extends Book {
    @JsonIgnore
    private SuggestionType suggestionType;
}