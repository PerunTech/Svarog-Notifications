import { React, connect, PropTypes, axios, createHashHistory, elements } from 'perun-core'
const { alertUser } = elements
import { iconManager } from './svgHolder';
import format from 'date-fns/format'
import en from 'date-fns/locale/en-US'
import { jsonToURI } from '../utils/utils';

const tableName = 'MESSAGE'

class MessagesComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {

    };
  }

  componentDidMount() {
    this.getInboxSubjectRecipientInfo()
  }

  getMessageSubject = () => {
    const { svSession, objId } = this.props
    const url = window.server + `/SvarogNotificationsServices/getObjectsByParentId/${svSession}/${objId}/${tableName}/PKID/DESC`
    axios.get(url)
      .then((response) => {
        if (response.data) {
          this.generateHtmlInboxSubject(response.data)

        }
      })
      .catch((error) => {
        console.error(error);
        alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
      })
  }


  getInboxSubjectRecipientInfo = () => {
    const { svSession } = this.props
    let url
    if (this.props.type === 'Sent') {
      url = window.server + `/SvarogNotificationsServices/getSentOrArchivedSubjectRecipientInfo/${svSession}/VALID`
    }
    else if (this.props.type === 'Archive') {
      url = window.server + `/SvarogNotificationsServices/getSentOrArchivedSubjectRecipientInfo/${svSession}/CLOSED`
    }
    else {
      url = window.server + `/SvarogNotificationsServices/getInboxSubjectRecipientInfo/${svSession}`
    }
    axios.get(url)
      .then((response) => {
        if (response.data) {
          this.iterateRecipientInfo(response.data)
          this.getMessageSubject()
        }
      })
      .catch((error) => {
        console.error(error);
        alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
      })
  }

  iterateRecipientInfo = (recipientData) => {
    const { objId } = this.props
    const recipientInfo = recipientData.filter(recipient => recipient.SUBJECT.object_id === objId)
    this.setState({ recipientState: recipientInfo })
    let recipients = []
    let recipientsObjIds = []
    let ccRecipients = []
    let ccRecipientsObjIds = []
    let bccRecipients = []
    let bccRecipientsObjIds = []
    for (let i = 0; i < recipientInfo.length; i++) {
      let subjectObjId = recipientInfo[i].SUBJECT.object_id
      let category = recipientInfo[i].SUBJECT.CATEGORY
      let priority = recipientInfo[i].SUBJECT.PRIORITY
      let moduleName = recipientInfo[i].SUBJECT.MODULE_NAME
      let title = recipientInfo[i].SUBJECT.TITLE
      this.setState({ subjectObjIdState: subjectObjId, category: category, priority: priority, moduleName: moduleName, title: title })
      if (recipientInfo[i] && recipientInfo[i].MSG_TO && recipientInfo[i].MSG_TO.items && Array.isArray(recipientInfo[i].MSG_TO.items) && recipientInfo[i].MSG_TO.items.length > 0) {
        for (let j = 0; j < recipientInfo[i].MSG_TO.items.length; j++) {
          recipients.push(recipientInfo[i].MSG_TO.items[j].USER_NAME)
          recipientsObjIds.push(recipientInfo[i].MSG_TO.items[j].object_id)
        }
        this.setState({ recipients, recipientsObjIds })
      }
      if (recipientInfo[i] && recipientInfo[i].MSG_CC && recipientInfo[i].MSG_CC.items && Array.isArray(recipientInfo[i].MSG_CC.items) && recipientInfo[i].MSG_CC.items.length > 0) {
        for (let k = 0; k < recipientInfo[i].MSG_CC.items.length; k++) {
          ccRecipients.push(recipientInfo[i].MSG_CC.items[k].USER_NAME)
          ccRecipientsObjIds.push(recipientInfo[i].MSG_CC.items[k].object_id)
        }
        this.setState({ ccRecipients, ccRecipientsObjIds })
      }
      if (recipientInfo[i] && recipientInfo[i].MSG_BCC && recipientInfo[i].MSG_BCC.items && Array.isArray(recipientInfo[i].MSG_BCC.items) && recipientInfo[i].MSG_BCC.items.length > 0) {
        for (let m = 0; m < recipientInfo[i].MSG_BCC.items.length; m++) {
          bccRecipients.push(recipientInfo[i].MSG_BCC.items[m].USER_NAME)
          bccRecipientsObjIds.push(recipientInfo[i].MSG_BCC.items[m].object_id)
        }
        this.setState({ bccRecipients, bccRecipientsObjIds })
      }
    }
  }
  generateHtmlInboxSubject = (subjectData) => {
    const { objId } = this.props
    let htmlSubjectElement
    let elementArr = []
    let className
    let labelCode
    if (subjectData) {
      for (let i = 0; i < subjectData.length; i++) {
        if (subjectData[i]["MESSAGE.PRIORITY"] === '1') {
          className = 'low-priority'
          labelCode = 'Low'
          this.setState({ className, labelCode });
        }
        if (subjectData[i]["MESSAGE.PRIORITY"] === '2') {
          className = 'normal-priority'
          labelCode = 'Normal'
          this.setState({ className, labelCode });
        }
        if (subjectData[i]["MESSAGE.PRIORITY"] === '3') {
          className = 'high-priority'
          labelCode = 'High'
          this.setState({ className, labelCode });
        }
        const createByName = subjectData[i]["MESSAGE.CREATED_BY_USERNAME"]
        const messageText = subjectData[i]["MESSAGE.TEXT"]
        const date = subjectData[i]["MESSAGE.DT_INSERT"]
        htmlSubjectElement = <div className={'message-holder'}>
          <div className={'message-data'}>
            <p type='text' className={'create-holder'}><b>{createByName}</b></p>
            <p type='text' className={'date-holder'}>
              {format(new Date(date), 'eee MMM dd kk:mm', { locale: en })}
            </p>

          </div>
          <div className={'message-subject-holder'}>
            <p className={'message-paragraph'}>Message: </p>
            <p type='text'>{messageText}</p>
          </div>
        </div>

        elementArr.push(htmlSubjectElement)
      }
      this.setState({ generatedValues: elementArr })
    }
  }

  replyFunc = (e) => {
    let htmlrReplyText
    let replayElementArr = []
    htmlrReplyText = <div className='reply-msg-holder'>
      <textarea name="messageText" id="messageText" onChange={this.onChange} className="reply-textarea" placeholder="Type your message here"></textarea>
      <button onClick={() => this.closeMessage()} className='cancel-btn'>Cancel</button>
    </div>
    replayElementArr.push(htmlrReplyText)

    this.setState({ generatedReplyValues: replayElementArr })

  }

  onChange = (e) => {
    this.setState({ [e.target.name]: e.target.value })
  }

  sentReply = (e) => {
    const { subjectObjIdState, messageText, recipientsObjIds, ccRecipientsObjIds, bccRecipientsObjIds, category, priority, moduleName, title } = this.state
    const replyData = { SUBJECT_OBJ_ID: subjectObjIdState, TEXT: messageText, CUSTOM_RECIPIENTS: '', CUSTOM_CC: '', CUSTOM_BCC: '', MSG_ATTACHMENT: '', PRIORITY: priority, CATEGORY: category, MODULE_NAME: moduleName, TITLE: title }
    if (Array.isArray(recipientsObjIds) && recipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_RECIPIENTS: recipientsObjIds.join(',') })
    }
    if (Array.isArray(ccRecipientsObjIds) && ccRecipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_CC: ccRecipientsObjIds.join(',') })
    }
    if (Array.isArray(bccRecipientsObjIds) && bccRecipientsObjIds.length > 0) {
      Object.assign(replyData, { CUSTOM_BCC: bccRecipientsObjIds.join(',') })
    }
    const data = jsonToURI(replyData)
    let { svSession } = this.props
    let postUrl = window.server + '/SvarogNotificationsServices/createNewMessage/' + svSession
    if (messageText == undefined || messageText === '') {
      alertUser(true, 'error', 'Enter a message')
    }
    else {
      axios({
        method: 'post',
        data: data,
        url: postUrl,
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
      }).then((response) => {
        if (response && response.data) {
          if (response.data) {
            if (response.data.type.toLowerCase() === 'success') {
              this.setState({ messageText: '', generatedReplyValues: '' }, () => this.getMessageSubject())
            }
          }
        }
      })
        .catch((error) => {
          console.error(error);
          alertUser(true, 'error', error.response?.data?.title || error, error.response?.data?.message || '');
        })
    }
  }


  closeMessage = () => {
    this.setState({ messageText: "" })
    this.setState({ generatedReplyValues: false })
  }

  render() {
    const { generatedValues, titleMsg, generatedReplyValues, title, recipients, ccRecipients, bccRecipients, labelCode, className } = this.state
    return (
      <React.Fragment>

        {titleMsg}
        <div className='context-menu-holder'>
          <p className='inbox-paragraph'>{title}</p>
          <p type='text' className={`priority-paragraph ${className}`}>Priority: {labelCode}</p>
          <div className='recipients' >
            <p type='text' className={'assigned-to'}>To: <strong className={'recipinets'}>{recipients?.join(', ')}</strong></p>
            {ccRecipients && ccRecipients.length > 0 && (
              <p type='text' className={'assigned-to'}>
                Cc: <strong className={'recipinets'}>{ccRecipients.join(', ')}</strong>
              </p>
            )}
            {bccRecipients && bccRecipients.length > 0 && (
              <p type='text' className={'assigned-to'}>
                Bcc: <strong className={'recipinets'}>{bccRecipients.join(', ')}</strong>
              </p>
            )}
          </div>
          <button className='btnBack' onClick={() => this.props.handleBack()}>{iconManager.getIcon('back')}Back</button>
          {generatedValues}
          {generatedReplyValues}
          <button onClick={(e) => generatedReplyValues ? this.sentReply(e) : this.replyFunc(e)} className='reply-msg'>Reply</button>
        </div>
      </React.Fragment>
    )
  }
}

const mapStateToProps = state => ({
  svSession: state.security.svSession
})

MessagesComponent.contextTypes = {
  intl: PropTypes.object.isRequired
}

export default connect(mapStateToProps)(MessagesComponent)