package com.example.shutthebox.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameEntry {

    private String id;
    private List<WoodenCard> woodenCards;
    private Integer dice1;
    private Integer dice2;
    private List<Player> players;
    private Integer playerTurnIndex;
    private Player loser;
    private String roomID;

}
