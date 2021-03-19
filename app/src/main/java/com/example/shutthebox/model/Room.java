package com.example.shutthebox.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    private String roomID;
    private List<Player> players;
    private String gameName;
    private Boolean available;
    private String gameEntryID;

}
