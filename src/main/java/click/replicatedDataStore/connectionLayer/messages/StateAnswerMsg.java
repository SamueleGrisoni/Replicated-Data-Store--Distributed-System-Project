package click.replicatedDataStore.connectionLayer.messages;

import click.replicatedDataStore.connectionLayer.CommunicationMethods;

public class StateAnswerMsg extends AbstractMsg<AnswerState>{
    public StateAnswerMsg(AnswerState state) {
        super(CommunicationMethods.ANSWER_STATE, state);
    }

    public AnswerState getPayload(){
        return (AnswerState) payLoad;
    }
}
