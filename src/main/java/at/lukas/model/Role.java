package at.lukas.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Role {
    private int id;
    private String name;
    private String prefix;
}
