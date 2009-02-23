package org.drools.process.command;

import org.drools.StatefulSession;
import org.drools.runtime.rule.WorkingMemoryEntryPoint;

public class GetWorkingMemoryEntryPointCommand
    implements
    Command<WorkingMemoryEntryPoint> {

    private String name;

    public GetWorkingMemoryEntryPointCommand(String name) {
        this.name = name;
    }

    public WorkingMemoryEntryPoint execute(StatefulSession session) {
        return session.getWorkingMemoryEntryPoint( name );
    }

    public String toString() {
        return "session.getWorkingMemoryEntryPoint( " + name + " );";
    }
}
